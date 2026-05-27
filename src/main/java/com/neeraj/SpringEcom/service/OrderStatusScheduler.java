package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.repo.OrderRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class OrderStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusScheduler.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);

    private final OrderRepo orderRepo;

    @Autowired(required = false)
    private SchedulerLockService schedulerLockService;

    public OrderStatusScheduler(OrderRepo orderRepo) {
        this.orderRepo = orderRepo;
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Kolkata")
    public void updateOrderStatuses() {
        runWithSchedulerLock("order-status", this::processOrderStatuses);
    }

    private void processOrderStatuses() {
        markFailedOnlineOrders();
        markEligibleOrdersAsDelivered();
    }

    private void runWithSchedulerLock(String lockName, Runnable task) {
        if (schedulerLockService == null) {
            task.run();
            return;
        }

        schedulerLockService.runWithLock(lockName, LOCK_TTL, task);
    }

    private void markFailedOnlineOrders() {
        List<Order> placedOrders = orderRepo.findByStatus(AppConstants.OrderStatus.PLACED);

        List<Order> failedOrders = placedOrders.stream()
                .filter(order -> order.getPaymentMode() == AppConstants.PaymentMode.ONLINE)
                .filter(order -> order.getPaymentStatus() == AppConstants.PaymentStatus.FAILED)
                .toList();

        for (Order order : failedOrders) {
            try {
                markOrderFailed(order.getId());
            } catch (Exception e) {
                log.error("Failed to mark order {} as failed during scheduled batch", order.getOrderId(), e);
            }
        }
    }

    private void markEligibleOrdersAsDelivered() {
        LocalDate sevenDaysAgo = LocalDate.now(BUSINESS_ZONE).minusDays(7);

        List<Order> oldPlacedOrders = orderRepo.findByStatusAndOrderDateBefore(
                AppConstants.OrderStatus.PLACED,
                sevenDaysAgo
        );

        List<Order> deliverableOrders = oldPlacedOrders.stream()
                .filter(this::canMarkDelivered)
                .toList();

        for (Order order : deliverableOrders) {
            try {
                markOrderDelivered(order.getId());
            } catch (Exception e) {
                log.error("Failed to mark order {} as delivered during scheduled batch", order.getOrderId(), e);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markOrderFailed(Long orderDbId) {
        Order order = orderRepo.findById(orderDbId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderDbId));

        if (order.getStatus() != AppConstants.OrderStatus.PLACED) {
            return;
        }

        if (order.getPaymentMode() != AppConstants.PaymentMode.ONLINE
                || order.getPaymentStatus() != AppConstants.PaymentStatus.FAILED) {
            return;
        }

        order.setStatus(AppConstants.OrderStatus.FAILED);
        orderRepo.save(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markOrderDelivered(Long orderDbId) {
        Order order = orderRepo.findById(orderDbId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderDbId));

        if (order.getStatus() != AppConstants.OrderStatus.PLACED) {
            return;
        }

        if (!canMarkDelivered(order)) {
            return;
        }

        order.setStatus(AppConstants.OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now(BUSINESS_ZONE));
        orderRepo.save(order);
    }

    private boolean canMarkDelivered(Order order) {
        if (order.getPaymentMode() == AppConstants.PaymentMode.CASH_ON_DELIVERY) {
            return true;
        }

        return order.getPaymentMode() == AppConstants.PaymentMode.ONLINE
                && order.getPaymentStatus() == AppConstants.PaymentStatus.PAID;
    }
}