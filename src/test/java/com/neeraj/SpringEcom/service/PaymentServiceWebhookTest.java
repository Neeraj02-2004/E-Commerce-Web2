package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.exception.InvalidOrderException;
import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.model.RazorpayWebhookEvent;
import com.neeraj.SpringEcom.model.ReturnExchangeRequest;
import com.neeraj.SpringEcom.payment.RazorpayGateway;
import com.neeraj.SpringEcom.repo.OrderRepo;
import com.neeraj.SpringEcom.repo.RazorpayWebhookEventRepo;
import com.neeraj.SpringEcom.repo.ReturnExchangeRepo;
import com.neeraj.SpringEcom.security.CurrentUserProvider;
import com.neeraj.SpringEcom.security.OrderOwnershipValidator;
import com.neeraj.SpringEcom.util.EmailNormalizer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceWebhookTest {

    private static final String WEBHOOK_SECRET = "test_webhook_secret";
    private static final EmailNormalizer EMAIL_NORMALIZER = new EmailNormalizer();

    @Test
    void handleRazorpayWebhook_whenSignatureMissing_shouldThrow() {
        PaymentService service = paymentService(
                mock(OrderRepo.class),
                mock(ReturnExchangeRepo.class),
                mock(RazorpayWebhookEventRepo.class)
        );

        assertThatThrownBy(() -> service.handleRazorpayWebhook(paymentCapturedPayload(), null))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Razorpay webhook signature is missing");
    }

    @Test
    void handleRazorpayWebhook_whenDuplicateEvent_shouldIgnore() throws Exception {
        OrderRepo orderRepo = mock(OrderRepo.class);
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        RazorpayWebhookEventRepo webhookEventRepo = mock(RazorpayWebhookEventRepo.class);

        when(webhookEventRepo.existsByEventId("evt_test_123")).thenReturn(true);

        PaymentService service = paymentService(orderRepo, returnExchangeRepo, webhookEventRepo);

        service.handleRazorpayWebhook(
                paymentCapturedPayload(),
                signatureFor(paymentCapturedPayload())
        );

        verify(webhookEventRepo, never()).saveAndFlush(any(RazorpayWebhookEvent.class));
        verifyNoInteractions(orderRepo);
    }

    @Test
    void handleRazorpayWebhook_paymentCaptured_shouldMarkOrderPaid() throws Exception {
        OrderRepo orderRepo = mock(OrderRepo.class);
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        RazorpayWebhookEventRepo webhookEventRepo = mock(RazorpayWebhookEventRepo.class);

        Order order = order("ORD123");
        order.setPaymentStatus(AppConstants.PaymentStatus.PENDING);

        when(webhookEventRepo.existsByEventId("evt_test_123")).thenReturn(false);
        when(webhookEventRepo.saveAndFlush(any(RazorpayWebhookEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepo.findByGatewayOrderId("order_test_123")).thenReturn(Optional.of(order));

        PaymentService service = paymentService(orderRepo, returnExchangeRepo, webhookEventRepo);

        service.handleRazorpayWebhook(
                paymentCapturedPayload(),
                signatureFor(paymentCapturedPayload())
        );

        assertThat(order.getPaymentMode()).isEqualTo(AppConstants.PaymentMode.ONLINE);
        assertThat(order.getPaymentStatus()).isEqualTo(AppConstants.PaymentStatus.PAID);
        assertThat(order.getGatewayPaymentId()).isEqualTo("pay_test_123");
        assertThat(order.getPaidAt()).isNotNull();

        verify(orderRepo).save(order);
    }

    @Test
    void handleRazorpayWebhook_paymentFailed_shouldMarkOrderFailedWhenNotPaid() throws Exception {
        OrderRepo orderRepo = mock(OrderRepo.class);
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        RazorpayWebhookEventRepo webhookEventRepo = mock(RazorpayWebhookEventRepo.class);

        Order order = order("ORD123");
        order.setPaymentStatus(AppConstants.PaymentStatus.PENDING);

        when(webhookEventRepo.existsByEventId("evt_test_failed")).thenReturn(false);
        when(webhookEventRepo.saveAndFlush(any(RazorpayWebhookEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepo.findByGatewayOrderId("order_test_123")).thenReturn(Optional.of(order));

        PaymentService service = paymentService(orderRepo, returnExchangeRepo, webhookEventRepo);

        service.handleRazorpayWebhook(
                paymentFailedPayload(),
                signatureFor(paymentFailedPayload())
        );

        assertThat(order.getPaymentMode()).isEqualTo(AppConstants.PaymentMode.ONLINE);
        assertThat(order.getPaymentStatus()).isEqualTo(AppConstants.PaymentStatus.FAILED);
        assertThat(order.getGatewayPaymentId()).isEqualTo("pay_failed_123");

        verify(orderRepo).save(order);
    }

    @Test
    void handleRazorpayWebhook_paymentFailed_shouldNotOverwritePaidOrder() throws Exception {
        OrderRepo orderRepo = mock(OrderRepo.class);
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        RazorpayWebhookEventRepo webhookEventRepo = mock(RazorpayWebhookEventRepo.class);

        Order order = order("ORD123");
        order.setPaymentStatus(AppConstants.PaymentStatus.PAID);

        when(webhookEventRepo.existsByEventId("evt_test_failed")).thenReturn(false);
        when(webhookEventRepo.saveAndFlush(any(RazorpayWebhookEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepo.findByGatewayOrderId("order_test_123")).thenReturn(Optional.of(order));

        PaymentService service = paymentService(orderRepo, returnExchangeRepo, webhookEventRepo);

        service.handleRazorpayWebhook(
                paymentFailedPayload(),
                signatureFor(paymentFailedPayload())
        );

        assertThat(order.getPaymentStatus()).isEqualTo(AppConstants.PaymentStatus.PAID);

        verify(orderRepo, never()).save(order);
    }

    @Test
    void handleRazorpayWebhook_refundProcessed_shouldMarkReturnExchangeRefundedAndCompleted() throws Exception {
        OrderRepo orderRepo = mock(OrderRepo.class);
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        RazorpayWebhookEventRepo webhookEventRepo = mock(RazorpayWebhookEventRepo.class);

        ReturnExchangeRequest request = new ReturnExchangeRequest();
        request.setRequestId("REX123");
        request.setStatus(AppConstants.ReturnExchangeStatus.APPROVED);
        request.setRefundStatus(AppConstants.RefundStatus.REFUND_PROCESSING);
        request.setGatewayRefundId("rfnd_test_123");

        when(webhookEventRepo.existsByEventId("evt_refund_processed")).thenReturn(false);
        when(webhookEventRepo.saveAndFlush(any(RazorpayWebhookEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(returnExchangeRepo.findByGatewayRefundId("rfnd_test_123")).thenReturn(Optional.of(request));

        PaymentService service = paymentService(orderRepo, returnExchangeRepo, webhookEventRepo);

        service.handleRazorpayWebhook(
                refundProcessedPayload(),
                signatureFor(refundProcessedPayload())
        );

        assertThat(request.getRefundStatus()).isEqualTo(AppConstants.RefundStatus.REFUNDED);
        assertThat(request.getStatus()).isEqualTo(AppConstants.ReturnExchangeStatus.COMPLETED);
        assertThat(request.getCompletedAt()).isNotNull();
        assertThat(request.getRefundProcessedAt()).isNotNull();
        assertThat(request.getRefundFailureReason()).isNull();

        verify(returnExchangeRepo).save(request);
    }

    private static PaymentService paymentService(
            OrderRepo orderRepo,
            ReturnExchangeRepo returnExchangeRepo,
            RazorpayWebhookEventRepo webhookEventRepo
    ) {
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        OrderOwnershipValidator orderOwnershipValidator = new OrderOwnershipValidator(EMAIL_NORMALIZER);
        RazorpayGateway razorpayGateway = mock(RazorpayGateway.class);

        return new PaymentService(
                orderRepo,
                returnExchangeRepo,
                webhookEventRepo,
                currentUserProvider,
                orderOwnershipValidator,
                razorpayGateway,
                "rzp_test_key",
                WEBHOOK_SECRET,
                "INR"
        );
    }

    private static String signatureFor(String payload) throws Exception {
        return hmacSha256(payload, WEBHOOK_SECRET);
    }

    private static String hmacSha256(String payload, String secret) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(keySpec);
        byte[] bytes = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        StringBuilder signature = new StringBuilder();

        for (byte b : bytes) {
            signature.append(String.format("%02x", b));
        }

        return signature.toString();
    }

    private static Order order(String orderId) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderId(orderId);
        order.setCustomerName("Neeraj Kumar");
        order.setEmail("buyer@example.com");
        order.setMobileNo("9876543210");
        order.setAddress("123 Main Road, Delhi, India");
        order.setPaymentMode(AppConstants.PaymentMode.ONLINE);
        order.setPaymentStatus(AppConstants.PaymentStatus.PENDING);
        order.setStatus(AppConstants.OrderStatus.PLACED);
        order.setOrderDate(LocalDate.now());
        order.setUserEmail("buyer@example.com");
        order.setGatewayOrderId("order_test_123");
        return order;
    }

    private static String paymentCapturedPayload() {
        return """
                {
                  "id": "evt_test_123",
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_test_123",
                        "order_id": "order_test_123"
                      }
                    }
                  }
                }
                """;
    }

    private static String paymentFailedPayload() {
        return """
                {
                  "id": "evt_test_failed",
                  "event": "payment.failed",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_failed_123",
                        "order_id": "order_test_123"
                      }
                    }
                  }
                }
                """;
    }

    private static String refundProcessedPayload() {
        return """
                {
                  "id": "evt_refund_processed",
                  "event": "refund.processed",
                  "payload": {
                    "refund": {
                      "entity": {
                        "id": "rfnd_test_123"
                      }
                    }
                  }
                }
                """;
    }
}