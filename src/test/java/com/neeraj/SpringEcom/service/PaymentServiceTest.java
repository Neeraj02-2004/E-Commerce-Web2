package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.exception.InvalidOrderException;
import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.model.OrderItem;
import com.neeraj.SpringEcom.model.Product;
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
import com.neeraj.SpringEcom.util.EmailNormalizer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private static final EmailNormalizer EMAIL_NORMALIZER = new EmailNormalizer();

    @Test
    void createPaymentOrder_forValidOrder_shouldCreateGatewayOrderAndPersistGatewayOrderId() {
        OrderRepo orderRepo = mock(OrderRepo.class);
        RazorpayGateway razorpayGateway = mock(RazorpayGateway.class);

        Order order = order("ORD123");
        order.setPaymentStatus(AppConstants.PaymentStatus.PENDING);

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));
        when(razorpayGateway.createOrder(10000L, "INR", "ORD123"))
                .thenReturn(new RazorpayGateway.GatewayOrder("order_test_123"));

        PaymentService service = service(orderRepo, razorpayGateway);

        PaymentCreateResponse response = service.createPaymentOrder(new PaymentCreateRequest("ORD123"));

        assertThat(response.keyId()).isEqualTo("rzp_test_key");
        assertThat(response.orderId()).isEqualTo("ORD123");
        assertThat(response.razorpayOrderId()).isEqualTo("order_test_123");
        assertThat(response.amount()).isEqualTo(10000L);
        assertThat(response.currency()).isEqualTo("INR");
        assertThat(order.getPaymentMode()).isEqualTo(AppConstants.PaymentMode.ONLINE);
        assertThat(order.getPaymentStatus()).isEqualTo(AppConstants.PaymentStatus.PENDING);
        assertThat(order.getGatewayOrderId()).isEqualTo("order_test_123");

        verify(orderRepo).save(order);
    }

    @Test
    void createPaymentOrder_forCancelledOrder_shouldThrowAndNotCallGateway() {
        OrderRepo orderRepo = mock(OrderRepo.class);
        RazorpayGateway razorpayGateway = mock(RazorpayGateway.class);

        Order order = order("ORD123");
        order.setStatus(AppConstants.OrderStatus.CANCELLED);

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));

        PaymentService service = service(orderRepo, razorpayGateway);

        assertThatThrownBy(() -> service.createPaymentOrder(new PaymentCreateRequest("ORD123")))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("Cannot pay for cancelled order");

        verifyNoInteractions(razorpayGateway);
        verify(orderRepo, never()).save(any());
    }

    @Test
    void createPaymentOrder_forAlreadyPaidOrder_shouldThrowAndNotCallGateway() {
        OrderRepo orderRepo = mock(OrderRepo.class);
        RazorpayGateway razorpayGateway = mock(RazorpayGateway.class);

        Order order = order("ORD123");
        order.setPaymentStatus(AppConstants.PaymentStatus.PAID);

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));

        PaymentService service = service(orderRepo, razorpayGateway);

        assertThatThrownBy(() -> service.createPaymentOrder(new PaymentCreateRequest("ORD123")))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("Order is already paid");

        verifyNoInteractions(razorpayGateway);
        verify(orderRepo, never()).save(any());
    }

    @Test
    void createPaymentOrder_whenGatewayFails_shouldThrowInvalidOrderException() {
        OrderRepo orderRepo = mock(OrderRepo.class);
        RazorpayGateway razorpayGateway = mock(RazorpayGateway.class);

        Order order = order("ORD123");

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));
        when(razorpayGateway.createOrder(10000L, "INR", "ORD123"))
                .thenThrow(new InvalidOrderException("Unable to create payment order"));

        PaymentService service = service(orderRepo, razorpayGateway);

        assertThatThrownBy(() -> service.createPaymentOrder(new PaymentCreateRequest("ORD123")))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("Unable to create payment order");

        verify(orderRepo, never()).save(any());
    }

    @Test
    void verifyPayment_withValidSignature_shouldMarkOrderPaid() {
        OrderRepo orderRepo = mock(OrderRepo.class);
        RazorpayGateway razorpayGateway = mock(RazorpayGateway.class);

        Order order = order("ORD123");
        order.setGatewayOrderId("order_test_123");

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));
        when(razorpayGateway.verifyPaymentSignature(
                "order_test_123",
                "pay_test_123",
                "valid_signature"
        )).thenReturn(true);

        PaymentService service = service(orderRepo, razorpayGateway);

        PaymentVerifyResponse response = service.verifyPayment(new PaymentVerifyRequest(
                "ORD123",
                "order_test_123",
                "pay_test_123",
                "valid_signature"
        ));

        assertThat(response.orderId()).isEqualTo("ORD123");
        assertThat(response.paymentStatus()).isEqualTo("PAID");
        assertThat(order.getPaymentStatus()).isEqualTo(AppConstants.PaymentStatus.PAID);
        assertThat(order.getGatewayPaymentId()).isEqualTo("pay_test_123");
        assertThat(order.getGatewaySignature()).isEqualTo("valid_signature");
        assertThat(order.getPaidAt()).isNotNull();

        verify(orderRepo).save(order);
    }

    @Test
    void verifyPayment_withInvalidSignature_shouldMarkFailedAndThrow() {
        OrderRepo orderRepo = mock(OrderRepo.class);
        RazorpayGateway razorpayGateway = mock(RazorpayGateway.class);

        Order order = order("ORD123");
        order.setGatewayOrderId("order_test_123");

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));
        when(razorpayGateway.verifyPaymentSignature(
                "order_test_123",
                "pay_test_123",
                "bad_signature"
        )).thenReturn(false);

        PaymentService service = service(orderRepo, razorpayGateway);

        assertThatThrownBy(() -> service.verifyPayment(new PaymentVerifyRequest(
                "ORD123",
                "order_test_123",
                "pay_test_123",
                "bad_signature"
        )))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("Invalid payment signature");

        assertThat(order.getPaymentStatus()).isEqualTo(AppConstants.PaymentStatus.FAILED);
        verify(orderRepo).save(order);
    }

    @Test
    void verifyPayment_whenGatewayOrderIdDoesNotMatch_shouldThrow() {
        OrderRepo orderRepo = mock(OrderRepo.class);
        RazorpayGateway razorpayGateway = mock(RazorpayGateway.class);

        Order order = order("ORD123");
        order.setGatewayOrderId("order_test_123");

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));

        PaymentService service = service(orderRepo, razorpayGateway);

        assertThatThrownBy(() -> service.verifyPayment(new PaymentVerifyRequest(
                "ORD123",
                "wrong_order_id",
                "pay_test_123",
                "signature"
        )))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("Payment order id does not match");

        verifyNoInteractions(razorpayGateway);
        verify(orderRepo, never()).save(any());
    }

    @Test
    void refundOnlinePayment_forPaidOnlineOrder_shouldCreateRefund() {
        RazorpayGateway razorpayGateway = mock(RazorpayGateway.class);

        Order order = order("ORD123");
        order.setPaymentMode(AppConstants.PaymentMode.ONLINE);
        order.setPaymentStatus(AppConstants.PaymentStatus.PAID);
        order.setGatewayPaymentId("pay_test_123");

        when(razorpayGateway.findRefundByIdempotencyKey("pay_test_123", "idem_123", 10000L))
                .thenReturn(Optional.empty());
        when(razorpayGateway.createRefund("pay_test_123", 10000L, "REX123", "idem_123"))
                .thenReturn(new RazorpayGateway.GatewayRefund("rfnd_test_123", 10000L));

        PaymentService service = service(mock(OrderRepo.class), razorpayGateway);

        PaymentService.RefundResult result = service.refundOnlinePayment(order, "REX123", "idem_123");

        assertThat(result.gatewayRefundId()).isEqualTo("rfnd_test_123");
        assertThat(result.refundAmount()).isEqualByComparingTo("100.00");
        assertThat(result.refundProcessedAt()).isNotNull();
    }

    @Test
    void refundOnlinePayment_whenMatchingRefundAlreadyExists_shouldReturnExistingRefund() {
        RazorpayGateway razorpayGateway = mock(RazorpayGateway.class);

        Order order = order("ORD123");
        order.setPaymentMode(AppConstants.PaymentMode.ONLINE);
        order.setPaymentStatus(AppConstants.PaymentStatus.PAID);
        order.setGatewayPaymentId("pay_test_123");

        when(razorpayGateway.findRefundByIdempotencyKey("pay_test_123", "idem_123", 10000L))
                .thenReturn(Optional.of(new RazorpayGateway.GatewayRefund("rfnd_existing", 10000L)));

        PaymentService service = service(mock(OrderRepo.class), razorpayGateway);

        PaymentService.RefundResult result = service.refundOnlinePayment(order, "REX123", "idem_123");

        assertThat(result.gatewayRefundId()).isEqualTo("rfnd_existing");
        assertThat(result.refundAmount()).isEqualByComparingTo("100.00");

        verify(razorpayGateway, never()).createRefund(any(), anyLong(), any(), any());
    }

    @Test
    void refundOnlinePayment_forNotPaidOrder_shouldThrowAndNotCallGateway() {
        RazorpayGateway razorpayGateway = mock(RazorpayGateway.class);

        Order order = order("ORD123");
        order.setPaymentMode(AppConstants.PaymentMode.ONLINE);
        order.setPaymentStatus(AppConstants.PaymentStatus.PENDING);
        order.setGatewayPaymentId("pay_test_123");

        PaymentService service = service(mock(OrderRepo.class), razorpayGateway);

        assertThatThrownBy(() -> service.refundOnlinePayment(order, "REX123", "idem_123"))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("Only paid orders can be refunded");

        verifyNoInteractions(razorpayGateway);
    }

    private static PaymentService service(OrderRepo orderRepo, RazorpayGateway razorpayGateway) {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        RazorpayWebhookEventRepo webhookEventRepo = mock(RazorpayWebhookEventRepo.class);
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        OrderOwnershipValidator orderOwnershipValidator = new OrderOwnershipValidator(EMAIL_NORMALIZER);

        when(currentUserProvider.getAuthenticatedEmail()).thenReturn("buyer@example.com");

        return new PaymentService(
                orderRepo,
                returnExchangeRepo,
                webhookEventRepo,
                currentUserProvider,
                orderOwnershipValidator,
                razorpayGateway,
                "rzp_test_key",
                "test_webhook_secret",
                "INR"
        );
    }

    private static Order order(String orderId) {
        Product product = new Product();
        product.setId(1L);
        product.setName("Phone");
        product.setPrice(new BigDecimal("100.00"));

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

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(1);
        item.setTotalPrice(new BigDecimal("100.00"));

        order.setOrderItems(List.of(item));
        return order;
    }
}