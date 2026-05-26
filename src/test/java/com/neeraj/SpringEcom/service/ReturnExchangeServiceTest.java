package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.exception.InvalidOrderException;
import com.neeraj.SpringEcom.exception.OrderNotFoundException;
import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.model.ReturnExchangeRequest;
import com.neeraj.SpringEcom.model.dto.ReturnExchangeCreateRequest;
import com.neeraj.SpringEcom.model.dto.ReturnExchangeDecisionRequest;
import com.neeraj.SpringEcom.model.dto.ReturnExchangeResponse;
import com.neeraj.SpringEcom.repo.OrderRepo;
import com.neeraj.SpringEcom.repo.ReturnExchangeRepo;
import com.neeraj.SpringEcom.security.CurrentUserProvider;
import com.neeraj.SpringEcom.security.OrderOwnershipValidator;
import com.neeraj.SpringEcom.util.EmailNormalizer;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReturnExchangeServiceTest {

    private static final EmailNormalizer EMAIL_NORMALIZER = new EmailNormalizer();

    @Test
    void createRequest_forDeliveredOrder_shouldCreateReturnRequest() {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        PaymentService paymentService = mock(PaymentService.class);

        Order order = deliveredOrder("ORD123", "buyer@example.com");

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));
        when(returnExchangeRepo.existsByOrderIdAndUserEmailAndStatusIn(
                any(),
                any(),
                anyList()
        )).thenReturn(false);

        when(returnExchangeRepo.save(any(ReturnExchangeRequest.class)))
                .thenAnswer(invocation -> {
                    ReturnExchangeRequest entity = invocation.getArgument(0);
                    entity.setId(1L);
                    entity.setCreatedAt(LocalDateTime.now());
                    entity.setUpdatedAt(LocalDateTime.now());
                    return entity;
                });

        ReturnExchangeService service = service(
                returnExchangeRepo,
                orderRepo,
                paymentService,
                "buyer@example.com"
        );

        ReturnExchangeResponse response = service.createRequest(
                "ORD123",
                new ReturnExchangeCreateRequest(
                        AppConstants.ReturnExchangeType.RETURN,
                        "Product is defective and not working"
                )
        );

        assertThat(response.requestId()).startsWith("REX");
        assertThat(response.orderId()).isEqualTo("ORD123");
        assertThat(response.userEmail()).isEqualTo("buyer@example.com");
        assertThat(response.requestType()).isEqualTo(AppConstants.ReturnExchangeType.RETURN);
        assertThat(response.status()).isEqualTo(AppConstants.ReturnExchangeStatus.REQUESTED);
        assertThat(response.refundStatus()).isEqualTo(AppConstants.RefundStatus.NOT_REQUIRED);
    }

    @Test
    void createRequest_forExchange_shouldCreateExchangeRequest() {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        PaymentService paymentService = mock(PaymentService.class);

        Order order = deliveredOrder("ORD123", "buyer@example.com");

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));
        when(returnExchangeRepo.existsByOrderIdAndUserEmailAndStatusIn(
                any(),
                any(),
                anyList()
        )).thenReturn(false);

        when(returnExchangeRepo.save(any(ReturnExchangeRequest.class)))
                .thenAnswer(invocation -> {
                    ReturnExchangeRequest entity = invocation.getArgument(0);
                    entity.setId(1L);
                    entity.setCreatedAt(LocalDateTime.now());
                    entity.setUpdatedAt(LocalDateTime.now());
                    return entity;
                });

        ReturnExchangeService service = service(
                returnExchangeRepo,
                orderRepo,
                paymentService,
                "buyer@example.com"
        );

        ReturnExchangeResponse response = service.createRequest(
                "ORD123",
                new ReturnExchangeCreateRequest(
                        AppConstants.ReturnExchangeType.EXCHANGE,
                        "Product size is not suitable for me"
                )
        );

        assertThat(response.requestType()).isEqualTo(AppConstants.ReturnExchangeType.EXCHANGE);
        assertThat(response.status()).isEqualTo(AppConstants.ReturnExchangeStatus.REQUESTED);
    }

    @Test
    void createRequest_forNonDeliveredOrder_shouldThrow() {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        PaymentService paymentService = mock(PaymentService.class);

        Order order = deliveredOrder("ORD123", "buyer@example.com");
        order.setStatus(AppConstants.OrderStatus.PLACED);

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));

        ReturnExchangeService service = service(
                returnExchangeRepo,
                orderRepo,
                paymentService,
                "buyer@example.com"
        );

        assertThatThrownBy(() -> service.createRequest(
                "ORD123",
                new ReturnExchangeCreateRequest(
                        AppConstants.ReturnExchangeType.RETURN,
                        "Product is defective and not working"
                )
        ))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Return/exchange is allowed only for delivered orders");
    }

    @Test
    void createRequest_afterReturnWindowExpired_shouldThrow() {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        PaymentService paymentService = mock(PaymentService.class);

        Order order = deliveredOrder("ORD123", "buyer@example.com");
        order.setDeliveredAt(LocalDateTime.now().minusDays(8));

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));

        ReturnExchangeService service = service(
                returnExchangeRepo,
                orderRepo,
                paymentService,
                "buyer@example.com"
        );

        assertThatThrownBy(() -> service.createRequest(
                "ORD123",
                new ReturnExchangeCreateRequest(
                        AppConstants.ReturnExchangeType.RETURN,
                        "Product is defective and not working"
                )
        ))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Return/exchange window has expired");
    }

    @Test
    void createRequest_forDifferentUserOrder_shouldThrowAccessDenied() {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        PaymentService paymentService = mock(PaymentService.class);

        Order order = deliveredOrder("ORD123", "owner@example.com");

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));

        ReturnExchangeService service = service(
                returnExchangeRepo,
                orderRepo,
                paymentService,
                "buyer@example.com"
        );

        assertThatThrownBy(() -> service.createRequest(
                "ORD123",
                new ReturnExchangeCreateRequest(
                        AppConstants.ReturnExchangeType.RETURN,
                        "Product is defective and not working"
                )
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You cannot create request for this order");
    }

    @Test
    void createRequest_forMissingOrder_shouldThrow() {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        PaymentService paymentService = mock(PaymentService.class);

        when(orderRepo.findByOrderId("ORD404")).thenReturn(Optional.empty());

        ReturnExchangeService service = service(
                returnExchangeRepo,
                orderRepo,
                paymentService,
                "buyer@example.com"
        );

        assertThatThrownBy(() -> service.createRequest(
                "ORD404",
                new ReturnExchangeCreateRequest(
                        AppConstants.ReturnExchangeType.RETURN,
                        "Product is defective and not working"
                )
        ))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void approveReturnRequest_forOnlinePaidOrder_shouldSetRefundProcessing() {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        PaymentService paymentService = mock(PaymentService.class);

        ReturnExchangeRequest entity = returnRequest(
                AppConstants.ReturnExchangeStatus.REQUESTED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.PAID
        );

        when(returnExchangeRepo.findByRequestId("REX123")).thenReturn(Optional.of(entity));
        when(returnExchangeRepo.save(any(ReturnExchangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReturnExchangeService service = service(
                returnExchangeRepo,
                orderRepo,
                paymentService,
                "buyer@example.com"
        );

        ReturnExchangeResponse response = service.approveRequest(
                "REX123",
                new ReturnExchangeDecisionRequest("Approved by admin")
        );

        assertThat(response.status()).isEqualTo(AppConstants.ReturnExchangeStatus.APPROVED);
        assertThat(response.refundStatus()).isEqualTo(AppConstants.RefundStatus.REFUND_PROCESSING);
        assertThat(response.adminNote()).isEqualTo("Approved by admin");
        assertThat(response.approvedAt()).isNotNull();
    }

    @Test
    void approveReturnRequest_forCodOrder_shouldSetManualRefundRequired() {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        PaymentService paymentService = mock(PaymentService.class);

        ReturnExchangeRequest entity = returnRequest(
                AppConstants.ReturnExchangeStatus.REQUESTED,
                AppConstants.PaymentMode.CASH_ON_DELIVERY,
                AppConstants.PaymentStatus.PENDING
        );

        when(returnExchangeRepo.findByRequestId("REX123")).thenReturn(Optional.of(entity));
        when(returnExchangeRepo.save(any(ReturnExchangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReturnExchangeService service = service(
                returnExchangeRepo,
                orderRepo,
                paymentService,
                "buyer@example.com"
        );

        ReturnExchangeResponse response = service.approveRequest(
                "REX123",
                new ReturnExchangeDecisionRequest("Approved by admin")
        );

        assertThat(response.status()).isEqualTo(AppConstants.ReturnExchangeStatus.APPROVED);
        assertThat(response.refundStatus()).isEqualTo(AppConstants.RefundStatus.MANUAL_REFUND_REQUIRED);
        assertThat(response.approvedAt()).isNotNull();
    }

    @Test
    void rejectRequest_shouldSetRejected() {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        PaymentService paymentService = mock(PaymentService.class);

        ReturnExchangeRequest entity = returnRequest(
                AppConstants.ReturnExchangeStatus.REQUESTED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.PAID
        );

        when(returnExchangeRepo.findByRequestId("REX123")).thenReturn(Optional.of(entity));
        when(returnExchangeRepo.save(any(ReturnExchangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReturnExchangeService service = service(
                returnExchangeRepo,
                orderRepo,
                paymentService,
                "buyer@example.com"
        );

        ReturnExchangeResponse response = service.rejectRequest(
                "REX123",
                new ReturnExchangeDecisionRequest("Rejected by admin")
        );

        assertThat(response.status()).isEqualTo(AppConstants.ReturnExchangeStatus.REJECTED);
        assertThat(response.refundStatus()).isEqualTo(AppConstants.RefundStatus.NOT_REQUIRED);
        assertThat(response.adminNote()).isEqualTo("Rejected by admin");
    }

    @Test
    void completeApprovedReturnRequest_shouldCallRazorpayRefundAndSetCompletedAndRefunded() {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        PaymentService paymentService = mock(PaymentService.class);

        ReturnExchangeRequest entity = returnRequest(
                AppConstants.ReturnExchangeStatus.APPROVED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.PAID
        );
        entity.setRefundStatus(AppConstants.RefundStatus.REFUND_PROCESSING);

        when(returnExchangeRepo.findByRequestId("REX123")).thenReturn(Optional.of(entity));
        when(returnExchangeRepo.saveAndFlush(any(ReturnExchangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentService.refundOnlinePayment(
                eq(entity.getOrder()),
                eq(entity.getRequestId()),
                anyString()
        ))
                .thenReturn(new PaymentService.RefundResult(
                        "rfnd_test_123",
                        new BigDecimal("100.00"),
                        LocalDateTime.now()
                ));
        when(returnExchangeRepo.save(any(ReturnExchangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReturnExchangeService service = service(
                returnExchangeRepo,
                orderRepo,
                paymentService,
                "buyer@example.com"
        );

        ReturnExchangeResponse response = service.completeRequest(
                "REX123",
                new ReturnExchangeDecisionRequest("Completed")
        );

        assertThat(response.status()).isEqualTo(AppConstants.ReturnExchangeStatus.COMPLETED);
        assertThat(response.refundStatus()).isEqualTo(AppConstants.RefundStatus.REFUNDED);
        assertThat(response.gatewayRefundId()).isEqualTo("rfnd_test_123");
        assertThat(response.refundAmount()).isEqualByComparingTo("100.00");
        assertThat(response.completedAt()).isNotNull();
        assertThat(response.refundProcessedAt()).isNotNull();
        assertThat(entity.getRefundIdempotencyKey()).isNotBlank();

        verify(returnExchangeRepo).saveAndFlush(entity);
        verify(paymentService).refundOnlinePayment(
                eq(entity.getOrder()),
                eq(entity.getRequestId()),
                eq(entity.getRefundIdempotencyKey())
        );
    }

    @Test
    void completeApprovedReturnRequest_whenRazorpayRefundFails_shouldSetRefundFailedAndKeepApproved() {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        PaymentService paymentService = mock(PaymentService.class);

        ReturnExchangeRequest entity = returnRequest(
                AppConstants.ReturnExchangeStatus.APPROVED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.PAID
        );
        entity.setRefundStatus(AppConstants.RefundStatus.REFUND_PROCESSING);

        when(returnExchangeRepo.findByRequestId("REX123")).thenReturn(Optional.of(entity));
        when(returnExchangeRepo.saveAndFlush(any(ReturnExchangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentService.refundOnlinePayment(
                eq(entity.getOrder()),
                eq(entity.getRequestId()),
                anyString()
        ))
                .thenThrow(new InvalidOrderException("Unable to process Razorpay refund"));
        when(returnExchangeRepo.save(any(ReturnExchangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReturnExchangeService service = service(
                returnExchangeRepo,
                orderRepo,
                paymentService,
                "buyer@example.com"
        );

        ReturnExchangeResponse response = service.completeRequest(
                "REX123",
                new ReturnExchangeDecisionRequest("Completed")
        );

        assertThat(response.status()).isEqualTo(AppConstants.ReturnExchangeStatus.APPROVED);
        assertThat(response.refundStatus()).isEqualTo(AppConstants.RefundStatus.REFUND_FAILED);
        assertThat(response.refundFailureReason()).isEqualTo("Unable to process Razorpay refund");
        assertThat(entity.getRefundIdempotencyKey()).isNotBlank();

        verify(returnExchangeRepo).saveAndFlush(entity);
    }

    @Test
    void completeApprovedRequestById_shouldLoadCompleteAndSaveRequest() {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        PaymentService paymentService = mock(PaymentService.class);

        ReturnExchangeRequest entity = returnRequest(
                AppConstants.ReturnExchangeStatus.APPROVED,
                AppConstants.PaymentMode.CASH_ON_DELIVERY,
                AppConstants.PaymentStatus.PENDING
        );
        entity.setRefundStatus(AppConstants.RefundStatus.MANUAL_REFUND_REQUIRED);

        when(returnExchangeRepo.findByRequestId("REX123")).thenReturn(Optional.of(entity));
        when(returnExchangeRepo.save(any(ReturnExchangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReturnExchangeService service = service(
                returnExchangeRepo,
                orderRepo,
                paymentService,
                "buyer@example.com"
        );

        service.completeApprovedRequestById("REX123", "Completed by scheduler");

        assertThat(entity.getStatus()).isEqualTo(AppConstants.ReturnExchangeStatus.COMPLETED);
        assertThat(entity.getRefundStatus()).isEqualTo(AppConstants.RefundStatus.REFUNDED);
        assertThat(entity.getAdminNote()).isEqualTo("Completed by scheduler");
        assertThat(entity.getCompletedAt()).isNotNull();
        assertThat(entity.getRefundProcessedAt()).isNotNull();

        verify(returnExchangeRepo).save(entity);
    }

    private static ReturnExchangeService service(
            ReturnExchangeRepo returnExchangeRepo,
            OrderRepo orderRepo,
            PaymentService paymentService,
            String currentUserEmail
    ) {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);

        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());

        when(currentUserProvider.getAuthenticatedEmail()).thenReturn(currentUserEmail);

        OrderOwnershipValidator orderOwnershipValidator = new OrderOwnershipValidator(EMAIL_NORMALIZER);

        return new ReturnExchangeService(
                returnExchangeRepo,
                orderRepo,
                paymentService,
                transactionManager,
                currentUserProvider,
                orderOwnershipValidator
        );
    }

    private static Order deliveredOrder(String orderId, String userEmail) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderId(orderId);
        order.setCustomerName("Neeraj Kumar");
        order.setEmail(userEmail);
        order.setMobileNo("9876543210");
        order.setAddress("123 Main Road, Delhi, India");
        order.setPaymentMode(AppConstants.PaymentMode.ONLINE);
        order.setPaymentStatus(AppConstants.PaymentStatus.PAID);
        order.setStatus(AppConstants.OrderStatus.DELIVERED);
        order.setOrderDate(LocalDate.now().minusDays(8));
        order.setDeliveredAt(LocalDateTime.now().minusDays(2));
        order.setUserEmail(userEmail);
        return order;
    }

    private static ReturnExchangeRequest returnRequest(
            String status,
            AppConstants.PaymentMode paymentMode,
            AppConstants.PaymentStatus paymentStatus
    ) {
        Order order = deliveredOrder("ORD123", "buyer@example.com");
        order.setPaymentMode(paymentMode);
        order.setPaymentStatus(paymentStatus);

        ReturnExchangeRequest entity = new ReturnExchangeRequest();
        entity.setId(1L);
        entity.setRequestId("REX123");
        entity.setOrder(order);
        entity.setOrderId(order.getOrderId());
        entity.setUserEmail(order.getUserEmail());
        entity.setRequestType(AppConstants.ReturnExchangeType.RETURN);
        entity.setReason("Product is defective and not working");
        entity.setStatus(status);
        entity.setRefundStatus(AppConstants.RefundStatus.NOT_REQUIRED);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
}