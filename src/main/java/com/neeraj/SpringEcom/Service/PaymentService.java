package com.neeraj.SpringEcom.Service;

import com.neeraj.SpringEcom.exception.InvalidOrderException;
import com.neeraj.SpringEcom.exception.OrderNotFoundException;
import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.model.OrderItem;
import com.neeraj.SpringEcom.model.dto.PaymentCreateRequest;
import com.neeraj.SpringEcom.model.dto.PaymentCreateResponse;
import com.neeraj.SpringEcom.model.dto.PaymentVerifyRequest;
import com.neeraj.SpringEcom.model.dto.PaymentVerifyResponse;
import com.neeraj.SpringEcom.repo.OrderRepo;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final OrderRepo orderRepo;
    private final RazorpayClient razorpayClient;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;
    private final String currency;

    public PaymentService(
            OrderRepo orderRepo,
            @Value("${razorpay.key-id}") String razorpayKeyId,
            @Value("${razorpay.key-secret}") String razorpayKeySecret,
            @Value("${razorpay.currency:INR}") String currency
    ) throws Exception {
        this.orderRepo = orderRepo;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
        this.currency = currency;
        this.razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
    }

    @Transactional
    public PaymentCreateResponse createPaymentOrder(PaymentCreateRequest request) {
        if (request == null || request.orderId() == null || request.orderId().isBlank()) {
            throw new InvalidOrderException("Order id is required");
        }

        String userEmail = getAuthenticatedEmail();

        Order order = orderRepo.findByOrderId(request.orderId())
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));

        if (!userEmail.equalsIgnoreCase(order.getUserEmail())) {
            throw new AccessDeniedException("You cannot pay for this order");
        }

        if (AppConstants.OrderStatus.CANCELLED.equals(order.getStatus())) {
            throw new InvalidOrderException("Cannot pay for cancelled order");
        }

        if (AppConstants.PaymentStatus.PAID.equals(order.getPaymentStatus())) {
            throw new InvalidOrderException("Order is already paid");
        }

        BigDecimal totalAmount = calculateOrderTotal(order);
        long amountInPaise = totalAmount.multiply(BigDecimal.valueOf(100)).longValueExact();

        try {
            JSONObject options = new JSONObject();
            options.put("amount", amountInPaise);
            options.put("currency", currency);
            options.put("receipt", order.getOrderId());
            options.put("payment_capture", 1);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(options);

            String razorpayOrderId = razorpayOrder.get("id");

            order.setPaymentMode(AppConstants.PaymentMode.ONLINE);
            order.setPaymentStatus(AppConstants.PaymentStatus.PENDING);
            order.setGatewayOrderId(razorpayOrderId);

            orderRepo.save(order);

            return new PaymentCreateResponse(
                    razorpayKeyId,
                    order.getOrderId(),
                    razorpayOrderId,
                    amountInPaise,
                    currency,
                    order.getCustomerName(),
                    order.getEmail(),
                    order.getMobileNo()
            );
        } catch (Exception e) {
            throw new InvalidOrderException("Unable to create payment order");
        }
    }

    @Transactional
    public PaymentVerifyResponse verifyPayment(PaymentVerifyRequest request) {
        validateVerifyRequest(request);

        String userEmail = getAuthenticatedEmail();

        Order order = orderRepo.findByOrderId(request.orderId())
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));

        if (!userEmail.equalsIgnoreCase(order.getUserEmail())) {
            throw new AccessDeniedException("You cannot verify this payment");
        }

        if (!request.razorpayOrderId().equals(order.getGatewayOrderId())) {
            throw new InvalidOrderException("Payment order id does not match");
        }

        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.razorpayOrderId());
            attributes.put("razorpay_payment_id", request.razorpayPaymentId());
            attributes.put("razorpay_signature", request.razorpaySignature());

            boolean validSignature = Utils.verifyPaymentSignature(
                    attributes,
                    razorpayKeySecret
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
                    order.getPaymentStatus(),
                    "Payment verified successfully"
            );
        } catch (InvalidOrderException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidOrderException("Payment verification failed");
        }
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

    private String getAuthenticatedEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new com.neeraj.SpringEcom.exception.UserNotAuthenticatedException("User not authenticated");
        }

        return auth.getName().toLowerCase().trim();
    }
}