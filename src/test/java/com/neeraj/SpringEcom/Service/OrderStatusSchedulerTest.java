package com.neeraj.SpringEcom.Service;

import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.repo.OrderRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatusSchedulerTest {

    @Mock
    private OrderRepo orderRepo;

    @InjectMocks
    private OrderStatusScheduler orderStatusScheduler;

    @Test
    void updateOrderStatuses_shouldDeliverOldCodOrders() {
        Order codOrder = order(
                AppConstants.OrderStatus.PLACED,
                AppConstants.PaymentMode.CASH_ON_DELIVERY,
                AppConstants.PaymentStatus.PENDING,
                LocalDate.now().minusDays(8)
        );

        when(orderRepo.findByStatus(AppConstants.OrderStatus.PLACED))
                .thenReturn(List.of());

        when(orderRepo.findByStatusAndOrderDateBefore(
                AppConstants.OrderStatus.PLACED,
                LocalDate.now().minusDays(7)
        )).thenReturn(List.of(codOrder));

        orderStatusScheduler.updateOrderStatuses();

        assertThat(codOrder.getStatus()).isEqualTo(AppConstants.OrderStatus.DELIVERED);
        verify(orderRepo, atLeastOnce()).saveAll(anyList());
    }

    @Test
    void updateOrderStatuses_shouldDeliverOldOnlinePaidOrders() {
        Order onlinePaidOrder = order(
                AppConstants.OrderStatus.PLACED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.PAID,
                LocalDate.now().minusDays(8)
        );

        when(orderRepo.findByStatus(AppConstants.OrderStatus.PLACED))
                .thenReturn(List.of());

        when(orderRepo.findByStatusAndOrderDateBefore(
                AppConstants.OrderStatus.PLACED,
                LocalDate.now().minusDays(7)
        )).thenReturn(List.of(onlinePaidOrder));

        orderStatusScheduler.updateOrderStatuses();

        assertThat(onlinePaidOrder.getStatus()).isEqualTo(AppConstants.OrderStatus.DELIVERED);
        verify(orderRepo, atLeastOnce()).saveAll(anyList());
    }

    @Test
    void updateOrderStatuses_shouldKeepOldOnlinePendingOrdersPlaced() {
        Order onlinePendingOrder = order(
                AppConstants.OrderStatus.PLACED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.PENDING,
                LocalDate.now().minusDays(8)
        );

        when(orderRepo.findByStatus(AppConstants.OrderStatus.PLACED))
                .thenReturn(List.of());

        when(orderRepo.findByStatusAndOrderDateBefore(
                AppConstants.OrderStatus.PLACED,
                LocalDate.now().minusDays(7)
        )).thenReturn(List.of(onlinePendingOrder));

        orderStatusScheduler.updateOrderStatuses();

        assertThat(onlinePendingOrder.getStatus()).isEqualTo(AppConstants.OrderStatus.PLACED);
        verify(orderRepo, atLeastOnce()).saveAll(anyList());
    }

    @Test
    void updateOrderStatuses_shouldMarkOnlineFailedOrdersAsFailed() {
        Order onlineFailedOrder = order(
                AppConstants.OrderStatus.PLACED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.FAILED,
                LocalDate.now()
        );

        when(orderRepo.findByStatus(AppConstants.OrderStatus.PLACED))
                .thenReturn(List.of(onlineFailedOrder));

        when(orderRepo.findByStatusAndOrderDateBefore(
                AppConstants.OrderStatus.PLACED,
                LocalDate.now().minusDays(7)
        )).thenReturn(List.of());

        orderStatusScheduler.updateOrderStatuses();

        assertThat(onlineFailedOrder.getStatus()).isEqualTo(AppConstants.OrderStatus.FAILED);

        ArgumentCaptor<List<Order>> captor = ArgumentCaptor.forClass(List.class);
        verify(orderRepo, atLeastOnce()).saveAll(captor.capture());

        assertThat(captor.getAllValues())
                .anySatisfy(savedOrders ->
                        assertThat(savedOrders).contains(onlineFailedOrder)
                );
    }

    @Test
    void updateOrderStatuses_shouldNotTouchCancelledOrders() {
        Order cancelledOrder = order(
                AppConstants.OrderStatus.CANCELLED,
                AppConstants.PaymentMode.ONLINE,
                AppConstants.PaymentStatus.PAID,
                LocalDate.now().minusDays(8)
        );

        when(orderRepo.findByStatus(AppConstants.OrderStatus.PLACED))
                .thenReturn(List.of());

        when(orderRepo.findByStatusAndOrderDateBefore(
                AppConstants.OrderStatus.PLACED,
                LocalDate.now().minusDays(7)
        )).thenReturn(List.of());

        orderStatusScheduler.updateOrderStatuses();

        assertThat(cancelledOrder.getStatus()).isEqualTo(AppConstants.OrderStatus.CANCELLED);
        verify(orderRepo, atLeastOnce()).saveAll(anyList());
    }

    private Order order(
            String status,
            String paymentMode,
            String paymentStatus,
            LocalDate orderDate
    ) {
        Order order = new Order();
        order.setStatus(status);
        order.setPaymentMode(paymentMode);
        order.setPaymentStatus(paymentStatus);
        order.setOrderDate(orderDate);
        return order;
    }
}