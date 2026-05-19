package com.neeraj.SpringEcom.Service;

import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.repo.OrderRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderStatusScheduler {

    private final OrderRepo orderRepo;

    public OrderStatusScheduler(OrderRepo orderRepo) {
        this.orderRepo = orderRepo;
    }

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void updateOrderStatuses() {
        markFailedOnlineOrders();
        markEligibleOrdersAsDelivered();
    }

    private void markFailedOnlineOrders() {
        List<Order> placedOrders = orderRepo.findByStatus(AppConstants.OrderStatus.PLACED);

        List<Order> failedOrders = placedOrders.stream()
                .filter(order -> AppConstants.PaymentMode.ONLINE.equals(order.getPaymentMode()))
                .filter(order -> AppConstants.PaymentStatus.FAILED.equals(order.getPaymentStatus()))
                .toList();

        if (failedOrders.isEmpty()) {
            return;
        }

        for (Order order : failedOrders) {
            order.setStatus(AppConstants.OrderStatus.FAILED);
        }

        orderRepo.saveAll(failedOrders);
    }

    private void markEligibleOrdersAsDelivered() {
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);

        List<Order> oldPlacedOrders = orderRepo.findByStatusAndOrderDateBefore(
                AppConstants.OrderStatus.PLACED,
                sevenDaysAgo
        );

        List<Order> deliverableOrders = oldPlacedOrders.stream()
                .filter(this::canMarkDelivered)
                .toList();

        if (deliverableOrders.isEmpty()) {
            return;
        }

        LocalDateTime deliveredTime = LocalDateTime.now();

        for (Order order : deliverableOrders) {
            order.setStatus(AppConstants.OrderStatus.DELIVERED);
            order.setDeliveredAt(deliveredTime);
        }

        orderRepo.saveAll(deliverableOrders);
    }

    private boolean canMarkDelivered(Order order) {
        if (AppConstants.PaymentMode.CASH_ON_DELIVERY.equals(order.getPaymentMode())) {
            return true;
        }

        return AppConstants.PaymentMode.ONLINE.equals(order.getPaymentMode())
                && AppConstants.PaymentStatus.PAID.equals(order.getPaymentStatus());
    }
}