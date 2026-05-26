package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.exception.InvalidOrderException;
import com.neeraj.SpringEcom.exception.OrderNotFoundException;
import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.model.OrderItem;
import com.neeraj.SpringEcom.model.RazorpayWebhookEvent;
import com.neeraj.SpringEcom.model.dto.PaymentCreateRequest;
import com.neeraj.SpringEcom.model.dto.PaymentCreateResponse;
import com.neeraj.SpringEcom.model.dto.PaymentVerifyRequest;
import com.neeraj.SpringEcom.model.dto.PaymentVerifyResponse;
import com.neeraj.SpringEcom.payment.RazorpayGateway;
import com.neeraj.SpringEcom.repo.OrderRepo;
import com.neeraj.SpringEcom.repo.RazorpayWebhookEventRepo;
import com.neeraj.SpringEcom.repo.ReturnExchangeRepo;
import com.neeraj.SpringEcom.security.CurrentUserProvider;
import com.neeraj.SpringEcom.security.OrderOwnershipValidator;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final OrderRepo orderRepo;
    private final ReturnExchangeRepo returnExchangeRepo;
    private final RazorpayWebhookEventRepo razorpayWebhookEventRepo;
    private final CurrentUserProvider currentUserProvider;
    private final OrderOwnershipValidator orderOwnershipValidator;
    private final RazorpayGateway razorpayGateway;
    private final String razorpayKeyId;
    private final String razorpayWebhookSecret;
    private final String currency;

    public PaymentService(
            OrderRepo orderRepo,
            ReturnExchangeRepo returnExchangeRepo,
            RazorpayWebhookEventRepo razorpayWebhookEventRepo,
            CurrentUserProvider currentUserProvider,
            OrderOwnershipValidator orderOwnershipValidator,
            RazorpayGateway razorpayGateway,
            @Value("${razorpay.key-id}") String razorpayKeyId,
            @Value("${razorpay.webhook-secret}") String razorpayWebhookSecret,
            @Value("${razorpay.currency:INR}") String currency
    ) {
        this.orderRepo = orderRepo;
        this.returnExchangeRepo = returnExchangeRepo;
        this.razorpayWebhookEventRepo = razorpayWebhookEventRepo;
        this.currentUserProvider = currentUserProvider;
        this.orderOwnershipValidator = orderOwnershipValidator;
        this.razorpayGateway = razorpayGateway;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayWebhookSecret = razorpayWebhookSecret;
        this.currency = currency;
    }

    @Transactional
    public PaymentCreateResponse createPaymentOrder(PaymentCreateRequest request) {
        if (request == null || request.orderId() == null || request.orderId().isBlank()) {
            throw new InvalidOrderException("Order id is required");
        }

        String userEmail = currentUserProvider.getAuthenticatedEmail();

        Order order = orderRepo.findByOrderId(request.orderId())
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));

        orderOwnershipValidator.assertOrderCanBePaidBy(order, userEmail);

        if (order.getStatus() == AppConstants.OrderStatus.CANCELLED) {
            throw new InvalidOrderException("Cannot pay for cancelled order");
        }

        if (order.getPaymentStatus() == AppConstants.PaymentStatus.PAID) {
            throw new InvalidOrderException("Order is already paid");
        }

        BigDecimal totalAmount = calculateOrderTotal(order);
        long amountInPaise = totalAmount.multiply(BigDecimal.valueOf(100)).longValueExact();

        try {
            RazorpayGateway.GatewayOrder gatewayOrder = razorpayGateway.createOrder(
                    amountInPaise,
                    currency,
                    order.getOrderId()
            );

            order.setPaymentMode(AppConstants.PaymentMode.ONLINE);
            order.setPaymentStatus(AppConstants.PaymentStatus.PENDING);
            order.setGatewayOrderId(gatewayOrder.id());

            orderRepo.save(order);

            return new PaymentCreateResponse(
                    razorpayKeyId,
                    order.getOrderId(),
                    gatewayOrder.id(),
                    amountInPaise,
                    currency,
                    order.getCustomerName(),
                    order.getEmail(),
                    order.getMobileNo()
            );
        } catch (InvalidOrderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Razorpay payment order creation failed for order {}", order.getOrderId(), e);
            throw new InvalidOrderException("Unable to create payment order");
        }
    }

    @Transactional
    public PaymentVerifyResponse verifyPayment(PaymentVerifyRequest request) {
        validateVerifyRequest(request);

        String userEmail = currentUserProvider.getAuthenticatedEmail();

        Order order = orderRepo.findByOrderId(request.orderId())
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));

        orderOwnershipValidator.assertOrderPaymentCanBeVerifiedBy(order, userEmail);

        if (!request.razorpayOrderId().equals(order.getGatewayOrderId())) {
            throw new InvalidOrderException("Payment order id does not match");
        }

        boolean validSignature = razorpayGateway.verifyPaymentSignature(
                request.razorpayOrderId(),
                request.razorpayPaymentId(),
                request.razorpaySignature()
        );

        if (!validSignature) {
            order.setPaymentStatus(AppConstants.PaymentStatus.FAILED);
            orderRepo.save(order);
            throw new InvalidOrderException("Invalid payment signature");
        }

        order.setPaymentMode(AppConstants.PaymentMode.ONLINE);
        order.setPaymentStatus(AppConstants.PaymentStatus.PAID);
        order.setGatewayPaymentId(request.razorpayPaymentId());
        order.setGatewaySignature(request.razorpaySignature());
        order.setPaidAt(LocalDateTime.now());

        orderRepo.save(order);

        return new PaymentVerifyResponse(
                order.getOrderId(),
                order.getPaymentStatus().name(),
                "Payment verified successfully"
        );
    }

    @Transactional
    public void handleRazorpayWebhook(String rawBody, String signature) {
        verifyWebhookSignature(rawBody, signature);

        JSONObject event = new JSONObject(rawBody);
        String eventId = getWebhookEventId(event, rawBody);
        String eventType = event.optString("event", "unknown");

        if (razorpayWebhookEventRepo.existsByEventId(eventId)) {
            return;
        }

        RazorpayWebhookEvent webhookEvent = new RazorpayWebhookEvent();
        webhookEvent.setEventId(eventId);
        webhookEvent.setEventType(eventType);
        webhookEvent.setRawBody(rawBody);

        try {
            razorpayWebhookEventRepo.saveAndFlush(webhookEvent);
        } catch (DataIntegrityViolationException e) {
            return;
        }

        switch (eventType) {
            case "payment.captured" -> handlePaymentCaptured(event);
            case "payment.failed" -> handlePaymentFailed(event);
            case "refund.created", "refund.speed_changed" -> handleRefundProcessing(event);
            case "refund.processed" -> handleRefundProcessed(event);
            case "refund.failed" -> handleRefundFailed(event);
            default -> {
            }
        }
    }

    public RefundResult refundOnlinePayment(Order order, String refundReceipt, String idempotencyKey) {
        validateRefundRequest(order, idempotencyKey);

        BigDecimal totalAmount = calculateOrderTotal(order);
        long amountInPaise = totalAmount.multiply(BigDecimal.valueOf(100)).longValueExact();
        String paymentId = order.getGatewayPaymentId();

        RazorpayGateway.GatewayRefund existingRefund = razorpayGateway
                .findRefundByIdempotencyKey(paymentId, idempotencyKey, amountInPaise)
                .orElse(null);

        if (existingRefund != null) {
            return toRefundResult(existingRefund);
        }

        try {
            RazorpayGateway.GatewayRefund refund = razorpayGateway.createRefund(
                    paymentId,
                    amountInPaise,
                    refundReceipt,
                    idempotencyKey
            );

            return toRefundResult(refund);
        } catch (InvalidOrderException e) {
            return razorpayGateway.findRefundByIdempotencyKey(paymentId, idempotencyKey, amountInPaise)
                    .map(this::toRefundResult)
                    .orElseThrow(() -> e);
        } catch (Exception e) {
            log.error("Razorpay refund failed for order {}", order.getOrderId(), e);

            return razorpayGateway.findRefundByIdempotencyKey(paymentId, idempotencyKey, amountInPaise)
                    .map(this::toRefundResult)
                    .orElseThrow(() -> new InvalidOrderException("Unable to process Razorpay refund"));
        }
    }

    private void verifyWebhookSignature(String rawBody, String signature) {
        if (razorpayWebhookSecret == null || razorpayWebhookSecret.isBlank()) {
            throw new InvalidOrderException("Razorpay webhook secret is not configured");
        }

        if (signature == null || signature.isBlank()) {
            throw new InvalidOrderException("Razorpay webhook signature is missing");
        }

        try {
            Utils.verifyWebhookSignature(rawBody, signature, razorpayWebhookSecret);
        } catch (Exception e) {
            throw new InvalidOrderException("Invalid Razorpay webhook signature");
        }
    }

    private String getWebhookEventId(JSONObject event, String rawBody) {
        String eventId = event.optString("id", null);

        if (eventId != null && !eventId.isBlank()) {
            return eventId;
        }

        return "sha256_" + sha256(rawBody);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new InvalidOrderException("Unable to process webhook event");
        }
    }

    private void handlePaymentCaptured(JSONObject event) {
        JSONObject payment = getPayloadEntity(event, "payment");
        String gatewayOrderId = payment.optString("order_id", null);
        String gatewayPaymentId = payment.optString("id", null);

        if (gatewayOrderId == null || gatewayOrderId.isBlank()) {
            return;
        }

        orderRepo.findByGatewayOrderId(gatewayOrderId).ifPresent(order -> {
            order.setPaymentMode(AppConstants.PaymentMode.ONLINE);
            order.setPaymentStatus(AppConstants.PaymentStatus.PAID);
            order.setGatewayPaymentId(gatewayPaymentId);
            order.setPaidAt(LocalDateTime.now());
            orderRepo.save(order);
        });
    }

    private void handlePaymentFailed(JSONObject event) {
        JSONObject payment = getPayloadEntity(event, "payment");
        String gatewayOrderId = payment.optString("order_id", null);
        String gatewayPaymentId = payment.optString("id", null);

        if (gatewayOrderId == null || gatewayOrderId.isBlank()) {
            return;
        }

        orderRepo.findByGatewayOrderId(gatewayOrderId).ifPresent(order -> {
            if (order.getPaymentStatus() != AppConstants.PaymentStatus.PAID) {
                order.setPaymentMode(AppConstants.PaymentMode.ONLINE);
                order.setPaymentStatus(AppConstants.PaymentStatus.FAILED);
                order.setGatewayPaymentId(gatewayPaymentId);
                orderRepo.save(order);
            }
        });
    }

    private void handleRefundProcessing(JSONObject event) {
        JSONObject refund = getPayloadEntity(event, "refund");
        String gatewayRefundId = refund.optString("id", null);

        if (gatewayRefundId == null || gatewayRefundId.isBlank()) {
            return;
        }

        returnExchangeRepo.findByGatewayRefundId(gatewayRefundId).ifPresent(request -> {
            request.setRefundStatus(AppConstants.RefundStatus.REFUND_PROCESSING);
            request.setRefundFailureReason(null);
            returnExchangeRepo.save(request);
        });
    }

    private void handleRefundProcessed(JSONObject event) {
        JSONObject refund = getPayloadEntity(event, "refund");
        String gatewayRefundId = refund.optString("id", null);

        if (gatewayRefundId == null || gatewayRefundId.isBlank()) {
            return;
        }

        returnExchangeRepo.findByGatewayRefundId(gatewayRefundId).ifPresent(request -> {
            request.setRefundStatus(AppConstants.RefundStatus.REFUNDED);
            request.setStatus(AppConstants.ReturnExchangeStatus.COMPLETED);
            request.setCompletedAt(LocalDateTime.now());
            request.setRefundProcessedAt(LocalDateTime.now());
            request.setRefundFailureReason(null);
            returnExchangeRepo.save(request);
        });
    }

    private void handleRefundFailed(JSONObject event) {
        JSONObject refund = getPayloadEntity(event, "refund");
        String gatewayRefundId = refund.optString("id", null);

        if (gatewayRefundId == null || gatewayRefundId.isBlank()) {
            return;
        }

        returnExchangeRepo.findByGatewayRefundId(gatewayRefundId).ifPresent(request -> {
            request.setRefundStatus(AppConstants.RefundStatus.REFUND_FAILED);
            request.setRefundFailureReason("Razorpay refund failed");
            returnExchangeRepo.save(request);
        });
    }

    private JSONObject getPayloadEntity(JSONObject event, String entityName) {
        return event
                .optJSONObject("payload")
                .optJSONObject(entityName)
                .optJSONObject("entity");
    }

    private void validateRefundRequest(Order order, String idempotencyKey) {
        if (order == null) {
            throw new InvalidOrderException("Order is required for refund");
        }

        if (order.getPaymentMode() != AppConstants.PaymentMode.ONLINE) {
            throw new InvalidOrderException("Only online payments can be refunded through Razorpay");
        }

        if (order.getPaymentStatus() != AppConstants.PaymentStatus.PAID) {
            throw new InvalidOrderException("Only paid orders can be refunded");
        }

        if (order.getGatewayPaymentId() == null || order.getGatewayPaymentId().isBlank()) {
            throw new InvalidOrderException("Razorpay payment id is missing");
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidOrderException("Refund idempotency key is missing");
        }
    }

    private RefundResult toRefundResult(RazorpayGateway.GatewayRefund refund) {
        return new RefundResult(
                refund.id(),
                BigDecimal.valueOf(refund.amountInPaise()).divide(BigDecimal.valueOf(100)),
                LocalDateTime.now()
        );
    }

    private void validateVerifyRequest(PaymentVerifyRequest request) {
        if (request == null) {
            throw new InvalidOrderException("Payment verification request is required");
        }

        if (request.orderId() == null || request.orderId().isBlank()) {
            throw new InvalidOrderException("Order id is required");
        }

        if (request.razorpayOrderId() == null || request.razorpayOrderId().isBlank()) {
            throw new InvalidOrderException("Razorpay order id is required");
        }

        if (request.razorpayPaymentId() == null || request.razorpayPaymentId().isBlank()) {
            throw new InvalidOrderException("Razorpay payment id is required");
        }

        if (request.razorpaySignature() == null || request.razorpaySignature().isBlank()) {
            throw new InvalidOrderException("Razorpay signature is required");
        }
    }

    private BigDecimal calculateOrderTotal(Order order) {
        return order.getOrderItems()
                .stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record RefundResult(
            String gatewayRefundId,
            BigDecimal refundAmount,
            LocalDateTime refundProcessedAt
    ) {
    }
}