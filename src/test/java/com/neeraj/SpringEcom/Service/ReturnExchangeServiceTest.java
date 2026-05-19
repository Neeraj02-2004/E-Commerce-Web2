package com.neeraj.SpringEcom.Service;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReturnExchangeServiceTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRequest_forDeliveredOrder_shouldCreateReturnRequest() {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);

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

        authenticate("buyer@example.com");

        ReturnExchangeService service = new ReturnExchangeService(returnExchangeRepo, orderRepo);

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

        authenticate("buyer@example.com");

        ReturnExchangeService service = new ReturnExchangeService(returnExchangeRepo, orderRepo);

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

        Order order = deliveredOrder("ORD123", "buyer@example.com");
        order.setStatus(AppConstants.OrderStatus.PLACED);

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));

        authenticate("buyer@example.com");

        ReturnExchangeService service = new ReturnExchangeService(returnExchangeRepo, orderRepo);

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

        Order order = deliveredOrder("ORD123", "buyer@example.com");
        order.setDeliveredAt(LocalDateTime.now().minusDays(8));

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));

        authenticate("buyer@example.com");

        ReturnExchangeService service = new ReturnExchangeService(returnExchangeRepo, orderRepo);

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

        Order order = deliveredOrder("ORD123", "owner@example.com");

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));

        authenticate("buyer@example.com");

        ReturnExchangeService service = new ReturnExchangeService(returnExchangeRepo, orderRepo);

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

        when(orderRepo.findByOrderId("ORD404")).thenReturn(Optional.empty());

        authenticate("buyer@example.com");

        ReturnExchangeService service = new ReturnExchangeService(returnExchangeRepo, orderRepo);

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

        ReturnExchangeRequest entity = returnRequest(
                AppConstants.ReturnExchangeStatus.REQUESTED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.PAID
        );

        when(returnExchangeRepo.findByRequestId("REX123")).thenReturn(Optional.of(entity));
        when(returnExchangeRepo.save(any(ReturnExchangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReturnExchangeService service = new ReturnExchangeService(returnExchangeRepo, orderRepo);

        ReturnExchangeResponse response = service.approveRequest(
                "REX123",
                new ReturnExchangeDecisionRequest("Approved by admin")
        );

        assertThat(response.status()).isEqualTo(AppConstants.ReturnExchangeStatus.APPROVED);
        assertThat(response.refundStatus()).isEqualTo(AppConstants.RefundStatus.REFUND_PROCESSING);
        assertThat(response.adminNote()).isEqualTo("Approved by admin");
    }

    @Test
    void approveReturnRequest_forCodOrder_shouldSetManualRefundRequired() {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);

        ReturnExchangeRequest entity = returnRequest(
                AppConstants.ReturnExchangeStatus.REQUESTED,
                AppConstants.PaymentMode.CASH_ON_DELIVERY,
                AppConstants.PaymentStatus.PENDING
        );

        when(returnExchangeRepo.findByRequestId("REX123")).thenReturn(Optional.of(entity));
        when(returnExchangeRepo.save(any(ReturnExchangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReturnExchangeService service = new ReturnExchangeService(returnExchangeRepo, orderRepo);

        ReturnExchangeResponse response = service.approveRequest(
                "REX123",
                new ReturnExchangeDecisionRequest("Approved by admin")
        );

        assertThat(response.status()).isEqualTo(AppConstants.ReturnExchangeStatus.APPROVED);
        assertThat(response.refundStatus()).isEqualTo(AppConstants.RefundStatus.MANUAL_REFUND_REQUIRED);
    }

    @Test
    void rejectRequest_shouldSetRejected() {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);

        ReturnExchangeRequest entity = returnRequest(
                AppConstants.ReturnExchangeStatus.REQUESTED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.PAID
        );

        when(returnExchangeRepo.findByRequestId("REX123")).thenReturn(Optional.of(entity));
        when(returnExchangeRepo.save(any(ReturnExchangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReturnExchangeService service = new ReturnExchangeService(returnExchangeRepo, orderRepo);

        ReturnExchangeResponse response = service.rejectRequest(
                "REX123",
                new ReturnExchangeDecisionRequest("Rejected by admin")
        );

        assertThat(response.status()).isEqualTo(AppConstants.ReturnExchangeStatus.REJECTED);
        assertThat(response.refundStatus()).isEqualTo(AppConstants.RefundStatus.NOT_REQUIRED);
        assertThat(response.adminNote()).isEqualTo("Rejected by admin");
    }

    @Test
    void completeApprovedReturnRequest_shouldSetCompletedAndRefunded() {
        ReturnExchangeRepo returnExchangeRepo = mock(ReturnExchangeRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);

        ReturnExchangeRequest entity = returnRequest(
                AppConstants.ReturnExchangeStatus.APPROVED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.PAID
        );
        entity.setRefundStatus(AppConstants.RefundStatus.REFUND_PROCESSING);

        when(returnExchangeRepo.findByRequestId("REX123")).thenReturn(Optional.of(entity));
        when(returnExchangeRepo.save(any(ReturnExchangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReturnExchangeService service = new ReturnExchangeService(returnExchangeRepo, orderRepo);

        ReturnExchangeResponse response = service.completeRequest(
                "REX123",
                new ReturnExchangeDecisionRequest("Completed")
        );

        assertThat(response.status()).isEqualTo(AppConstants.ReturnExchangeStatus.COMPLETED);
        assertThat(response.refundStatus()).isEqualTo(AppConstants.RefundStatus.REFUNDED);
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of())
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
            String paymentMode,
            String paymentStatus
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