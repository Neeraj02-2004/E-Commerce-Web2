package com.neeraj.SpringEcom.Service;

import com.neeraj.SpringEcom.exception.InsufficientStockException;
import com.neeraj.SpringEcom.exception.InvalidOrderException;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.model.dto.OrderItemRequest;
import com.neeraj.SpringEcom.model.dto.OrderRequest;
import com.neeraj.SpringEcom.repo.OrderRepo;
import com.neeraj.SpringEcom.repo.ProductRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void placeOrder_whenProductIsUnavailable_shouldThrowAndNotSaveOrder() {
        Product product = product(1, "Phone", 5);
        product.setProductAvailable(false);

        ProductRepo productRepo = mock(ProductRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);

        when(productRepo.findByIdForUpdate(1)).thenReturn(Optional.of(product));

        OrderService orderService = new OrderService(productRepo, orderRepo);
        authenticate("buyer@example.com");

        assertThatThrownBy(() -> orderService.placeOrder(orderRequest(1, 1)))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Product is not available");

        assertThat(product.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void placeOrder_whenStockIsInsufficient_shouldThrowAndNotSaveOrder() {
        Product product = product(1, "Phone", 2);
        ReentrantLock rowLock = new ReentrantLock();

        ProductRepo productRepo = mock(ProductRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        List<Order> savedOrders = new ArrayList<>();

        when(productRepo.findByIdForUpdate(1)).thenAnswer(invocation -> {
            rowLock.lock();
            return Optional.of(product);
        });

        when(productRepo.saveAll(any())).thenAnswer(invocation -> {
            unlockIfHeld(rowLock);
            return List.of();
        });

        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId((long) savedOrders.size() + 1);
            savedOrders.add(order);
            return order;
        });

        OrderService orderService = new OrderService(productRepo, orderRepo);
        authenticate("buyer@example.com");

        assertThatThrownBy(() -> orderService.placeOrder(orderRequest(1, 3)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");

        unlockIfHeld(rowLock);

        assertThat(product.getStockQuantity()).isEqualTo(2);
        assertThat(savedOrders).isEmpty();
    }

    @Test
    void placeOrder_whenTwoConcurrentOrdersCompeteForOneStock_shouldOnlyPlaceOneOrder() throws Exception {
        Product product = product(1, "Phone", 1);
        ReentrantLock rowLock = new ReentrantLock();

        ProductRepo productRepo = mock(ProductRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        List<Order> savedOrders = java.util.Collections.synchronizedList(new ArrayList<>());

        when(productRepo.findByIdForUpdate(1)).thenAnswer(invocation -> {
            rowLock.lock();
            return Optional.of(product);
        });

        when(productRepo.saveAll(any())).thenAnswer(invocation -> {
            unlockIfHeld(rowLock);
            return List.of();
        });

        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            synchronized (savedOrders) {
                order.setId((long) savedOrders.size() + 1);
                savedOrders.add(order);
            }
            return order;
        });

        OrderService orderService = new OrderService(productRepo, orderRepo);

        int threadCount = 2;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger insufficientStockCount = new AtomicInteger();

        try (var executor = Executors.newFixedThreadPool(threadCount)) {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    authenticate("buyer@example.com");
                    ready.countDown();

                    try {
                        start.await();
                        orderService.placeOrder(orderRequest(1, 1));
                        successCount.incrementAndGet();
                    } catch (InsufficientStockException e) {
                        insufficientStockCount.incrementAndGet();
                        unlockIfHeld(rowLock);
                    } catch (Exception e) {
                        unlockIfHeld(rowLock);
                        throw new RuntimeException(e);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }

            ready.await();
            start.countDown();

            for (Future<?> future : futures) {
                future.get();
            }
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(insufficientStockCount.get()).isEqualTo(1);
        assertThat(product.getStockQuantity()).isZero();
        assertThat(savedOrders).hasSize(1);
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of())
        );
    }

    private static void unlockIfHeld(ReentrantLock rowLock) {
        if (rowLock.isHeldByCurrentThread()) {
            rowLock.unlock();
        }
    }

    private static OrderRequest orderRequest(int productId, int quantity) {
        return new OrderRequest(
                "Neeraj Kumar",
                "buyer@example.com",
                "9876543210",
                "123 Main Road, Delhi, India",
                "CASH_ON_DELIVERY",
                List.of(new OrderItemRequest(productId, quantity))
        );
    }
    private static Product product(int id, String name, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setBrand("Test Brand");
        product.setDescription("Test Description");
        product.setCategory("Mobiles");
        product.setPrice(new BigDecimal("100.00"));
        product.setProductAvailable(true);
        product.setStockQuantity(stock);
        return product;
    }
}