package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.repo.OrderRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatusSchedulerTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    @Mock
    private OrderRepo orderRepo;

    @InjectMocks
    private OrderStatusScheduler orderStatusScheduler;

    @Test
    void updateOrderStatuses_shouldRunInAsiaKolkataZone() throws Exception {
        Scheduled scheduled = OrderStatusScheduler.class
                .getMethod("updateOrderStatuses")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.zone()).isEqualTo("Asia/Kolkata");
    }

    @Test
    void updateOrderStatuses_shouldDeliverOldCodOrders() {
        LocalDate sevenDaysAgo = LocalDate.now(BUSINESS_ZONE).minusDays(7);

        Order codOrder = order(
                1L,
                AppConstants.OrderStatus.PLACED,
                AppConstants.PaymentMode.CASH_ON_DELIVERY,
                AppConstants.PaymentStatus.PENDING,
                sevenDaysAgo.minusDays(1)
        );

        when(orderRepo.findByStatus(AppConstants.OrderStatus.PLACED))
                .thenReturn(List.of());

        when(orderRepo.findByStatusAndOrderDateBefore(
                AppConstants.OrderStatus.PLACED,
                sevenDaysAgo
        )).thenReturn(List.of(codOrder));

        when(orderRepo.findById(1L)).thenReturn(Optional.of(codOrder));
        when(orderRepo.save(codOrder)).thenReturn(codOrder);

        orderStatusScheduler.updateOrderStatuses();

        assertThat(codOrder.getStatus()).isEqualTo(AppConstants.OrderStatus.DELIVERED);
        assertThat(codOrder.getDeliveredAt()).isNotNull();

        verify(orderRepo).save(codOrder);
        verify(orderRepo, never()).saveAll(anyList());
    }

    @Test
    void updateOrderStatuses_shouldDeliverOldOnlinePaidOrders() {
        LocalDate sevenDaysAgo = LocalDate.now(BUSINESS_ZONE).minusDays(7);

        Order onlinePaidOrder = order(
                1L,
                AppConstants.OrderStatus.PLACED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.PAID,
                sevenDaysAgo.minusDays(1)
        );

        when(orderRepo.findByStatus(AppConstants.OrderStatus.PLACED))
                .thenReturn(List.of());

        when(orderRepo.findByStatusAndOrderDateBefore(
                AppConstants.OrderStatus.PLACED,
                sevenDaysAgo
        )).thenReturn(List.of(onlinePaidOrder));

        when(orderRepo.findById(1L)).thenReturn(Optional.of(onlinePaidOrder));
        when(orderRepo.save(onlinePaidOrder)).thenReturn(onlinePaidOrder);

        orderStatusScheduler.updateOrderStatuses();

        assertThat(onlinePaidOrder.getStatus()).isEqualTo(AppConstants.OrderStatus.DELIVERED);
        assertThat(onlinePaidOrder.getDeliveredAt()).isNotNull();

        verify(orderRepo).save(onlinePaidOrder);
        verify(orderRepo, never()).saveAll(anyList());
    }

    @Test
    void updateOrderStatuses_shouldKeepOldOnlinePendingOrdersPlaced() {
        LocalDate sevenDaysAgo = LocalDate.now(BUSINESS_ZONE).minusDays(7);

        Order onlinePendingOrder = order(
                1L,
                AppConstants.OrderStatus.PLACED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.PENDING,
                sevenDaysAgo.minusDays(1)
        );

        when(orderRepo.findByStatus(AppConstants.OrderStatus.PLACED))
                .thenReturn(List.of());

        when(orderRepo.findByStatusAndOrderDateBefore(
                AppConstants.OrderStatus.PLACED,
                sevenDaysAgo
        )).thenReturn(List.of(onlinePendingOrder));

        orderStatusScheduler.updateOrderStatuses();

        assertThat(onlinePendingOrder.getStatus()).isEqualTo(AppConstants.OrderStatus.PLACED);
        assertThat(onlinePendingOrder.getDeliveredAt()).isNull();

        verify(orderRepo, never()).save(any());
        verify(orderRepo, never()).saveAll(anyList());
    }

    @Test
    void updateOrderStatuses_shouldMarkOnlineFailedOrdersAsFailed() {
        LocalDate sevenDaysAgo = LocalDate.now(BUSINESS_ZONE).minusDays(7);

        Order onlineFailedOrder = order(
                1L,
                AppConstants.OrderStatus.PLACED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.FAILED,
                LocalDate.now(BUSINESS_ZONE)
        );

        when(orderRepo.findByStatus(AppConstants.OrderStatus.PLACED))
                .thenReturn(List.of(onlineFailedOrder));

        when(orderRepo.findByStatusAndOrderDateBefore(
                AppConstants.OrderStatus.PLACED,
                sevenDaysAgo
        )).thenReturn(List.of());

        when(orderRepo.findById(1L)).thenReturn(Optional.of(onlineFailedOrder));
        when(orderRepo.save(onlineFailedOrder)).thenReturn(onlineFailedOrder);

        orderStatusScheduler.updateOrderStatuses();

        assertThat(onlineFailedOrder.getStatus()).isEqualTo(AppConstants.OrderStatus.FAILED);
        assertThat(onlineFailedOrder.getDeliveredAt()).isNull();

        verify(orderRepo).save(onlineFailedOrder);
        verify(orderRepo, never()).saveAll(anyList());
    }

    @Test
    void updateOrderStatuses_shouldNotTouchCancelledOrders() {
        LocalDate sevenDaysAgo = LocalDate.now(BUSINESS_ZONE).minusDays(7);

        Order cancelledOrder = order(
                1L,
                AppConstants.OrderStatus.CANCELLED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.PAID,
                sevenDaysAgo.minusDays(1)
        );

        when(orderRepo.findByStatus(AppConstants.OrderStatus.PLACED))
                .thenReturn(List.of());

        when(orderRepo.findByStatusAndOrderDateBefore(
                AppConstants.OrderStatus.PLACED,
                sevenDaysAgo
        )).thenReturn(List.of());

        orderStatusScheduler.updateOrderStatuses();

        assertThat(cancelledOrder.getStatus()).isEqualTo(AppConstants.OrderStatus.CANCELLED);
        assertThat(cancelledOrder.getDeliveredAt()).isNull();

        verify(orderRepo, never()).save(any());
        verify(orderRepo, never()).saveAll(anyList());
    }

    @Test
    void updateOrderStatuses_whenOneOrderFails_shouldContinueNextOrder() {
        LocalDate sevenDaysAgo = LocalDate.now(BUSINESS_ZONE).minusDays(7);

        Order firstOrder = order(
                1L,
                AppConstants.OrderStatus.PLACED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.FAILED,
                LocalDate.now(BUSINESS_ZONE)
        );

        Order secondOrder = order(
                2L,
                AppConstants.OrderStatus.PLACED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.FAILED,
                LocalDate.now(BUSINESS_ZONE)
        );

        when(orderRepo.findByStatus(AppConstants.OrderStatus.PLACED))
                .thenReturn(List.of(firstOrder, secondOrder));

        when(orderRepo.findByStatusAndOrderDateBefore(
                AppConstants.OrderStatus.PLACED,
                sevenDaysAgo
        )).thenReturn(List.of());

        when(orderRepo.findById(1L)).thenThrow(new RuntimeException("Temporary failure"));
        when(orderRepo.findById(2L)).thenReturn(Optional.of(secondOrder));
        when(orderRepo.save(secondOrder)).thenReturn(secondOrder);

        orderStatusScheduler.updateOrderStatuses();

        assertThat(secondOrder.getStatus()).isEqualTo(AppConstants.OrderStatus.FAILED);

        verify(orderRepo).findById(1L);
        verify(orderRepo).findById(2L);
        verify(orderRepo).save(secondOrder);
        verify(orderRepo, never()).saveAll(anyList());
    }

    @Test
    void markOrderDelivered_shouldIgnoreOrderThatIsNoLongerPlaced() {
        Order shippedOrder = order(
                1L,
                AppConstants.OrderStatus.SHIPPED,
                AppConstants.PaymentMode.CASH_ON_DELIVERY,
                AppConstants.PaymentStatus.PENDING,
                LocalDate.now(BUSINESS_ZONE).minusDays(8)
        );

        when(orderRepo.findById(1L)).thenReturn(Optional.of(shippedOrder));

        orderStatusScheduler.markOrderDelivered(1L);

        assertThat(shippedOrder.getStatus()).isEqualTo(AppConstants.OrderStatus.SHIPPED);
        assertThat(shippedOrder.getDeliveredAt()).isNull();

        verify(orderRepo, never()).save(any());
    }

    private Order order(
            Long id,
            AppConstants.OrderStatus status,
            AppConstants.PaymentMode paymentMode,
            AppConstants.PaymentStatus paymentStatus,
            LocalDate orderDate
    ) {
        Order order = new Order();
        order.setId(id);
        order.setOrderId("ORD" + id);
        order.setStatus(status);
        order.setPaymentMode(paymentMode);
        order.setPaymentStatus(paymentStatus);
        order.setOrderDate(orderDate);
        return order;
    }
}