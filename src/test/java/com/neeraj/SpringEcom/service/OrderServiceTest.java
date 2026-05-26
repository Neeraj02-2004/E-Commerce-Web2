package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.exception.InsufficientStockException;
import com.neeraj.SpringEcom.exception.InvalidOrderException;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.model.User;
import com.neeraj.SpringEcom.model.dto.OrderItemRequest;
import com.neeraj.SpringEcom.model.dto.OrderRequest;
import com.neeraj.SpringEcom.repo.OrderRepo;
import com.neeraj.SpringEcom.repo.ProductRepo;
import com.neeraj.SpringEcom.repo.UserRepo;
import com.neeraj.SpringEcom.security.CurrentUserProvider;
import com.neeraj.SpringEcom.security.OrderOwnershipValidator;
import com.neeraj.SpringEcom.util.EmailNormalizer;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.cache.CacheManager;

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
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private static final EmailNormalizer EMAIL_NORMALIZER = new EmailNormalizer();

    @Test
    void placeOrder_whenProductIsUnavailable_shouldThrowAndNotSaveOrder() {
        Product product = product(1L, "Phone", 5);
        product.setProductAvailable(false);

        ProductRepo productRepo = mock(ProductRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        UserRepo userRepo = userRepo("buyer@example.com");
        CurrentUserProvider currentUserProvider = currentUserProvider("buyer@example.com");
        OrderOwnershipValidator orderOwnershipValidator = new OrderOwnershipValidator(EMAIL_NORMALIZER);
        CacheManager cacheManager = mock(CacheManager.class);

        when(productRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        OrderService orderService = new OrderService(
                productRepo,
                orderRepo,
                userRepo,
                currentUserProvider,
                orderOwnershipValidator,
                cacheManager,
                EMAIL_NORMALIZER
        );

        assertThatThrownBy(() -> orderService.placeOrder(orderRequest(1L, 1)))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Product is not available");

        assertThat(product.getStockQuantity()).isEqualTo(5);
        verify(orderRepo, never()).save(any());
    }

    @Test
    void placeOrder_whenStockIsInsufficient_shouldThrowAndNotSaveOrder() {
        Product product = product(1L, "Phone", 2);
        ReentrantLock rowLock = new ReentrantLock();

        ProductRepo productRepo = mock(ProductRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        UserRepo userRepo = userRepo("buyer@example.com");
        CurrentUserProvider currentUserProvider = currentUserProvider("buyer@example.com");
        OrderOwnershipValidator orderOwnershipValidator = new OrderOwnershipValidator(EMAIL_NORMALIZER);
        CacheManager cacheManager = mock(CacheManager.class);
        List<Order> savedOrders = new ArrayList<>();

        when(productRepo.findByIdForUpdate(1L)).thenAnswer(invocation -> {
            rowLock.lock();
            return Optional.of(product);
        });

        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            unlockIfHeld(rowLock);
            Order order = invocation.getArgument(0);
            order.setId((long) savedOrders.size() + 1);
            savedOrders.add(order);
            return order;
        });

        OrderService orderService = new OrderService(
                productRepo,
                orderRepo,
                userRepo,
                currentUserProvider,
                orderOwnershipValidator,
                cacheManager,
                EMAIL_NORMALIZER
        );

        assertThatThrownBy(() -> orderService.placeOrder(orderRequest(1L, 3)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");

        unlockIfHeld(rowLock);

        assertThat(product.getStockQuantity()).isEqualTo(2);
        assertThat(savedOrders).isEmpty();
        verify(orderRepo, never()).save(any());
    }

    @Test
    void placeOrder_withMultipleItems_shouldLockProductsInAscendingProductIdOrder() {
        Product firstProduct = product(1L, "Mouse", 5);
        Product secondProduct = product(2L, "Keyboard", 5);

        ProductRepo productRepo = mock(ProductRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        UserRepo userRepo = userRepo("buyer@example.com");
        CurrentUserProvider currentUserProvider = currentUserProvider("buyer@example.com");
        OrderOwnershipValidator orderOwnershipValidator = new OrderOwnershipValidator(EMAIL_NORMALIZER);
        CacheManager cacheManager = mock(CacheManager.class);

        when(productRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(firstProduct));
        when(productRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(secondProduct));
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        OrderService orderService = new OrderService(
                productRepo,
                orderRepo,
                userRepo,
                currentUserProvider,
                orderOwnershipValidator,
                cacheManager,
                EMAIL_NORMALIZER
        );

        OrderRequest request = new OrderRequest(
                "Neeraj Kumar",
                "buyer@example.com",
                "9876543210",
                "123 Main Road, Delhi, India",
                "CASH_ON_DELIVERY",
                List.of(
                        new OrderItemRequest(2L, 1),
                        new OrderItemRequest(1L, 1)
                )
        );

        orderService.placeOrder(request);

        InOrder inOrder = inOrder(productRepo);
        inOrder.verify(productRepo).findByIdForUpdate(1L);
        inOrder.verify(productRepo).findByIdForUpdate(2L);
    }

    @Test
    void placeOrder_whenTwoConcurrentOrdersCompeteForOneStock_shouldOnlyPlaceOneOrder() throws Exception {
        Product product = product(1L, "Phone", 1);
        ReentrantLock rowLock = new ReentrantLock();

        ProductRepo productRepo = mock(ProductRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        UserRepo userRepo = userRepo("buyer@example.com");
        CurrentUserProvider currentUserProvider = currentUserProvider("buyer@example.com");
        OrderOwnershipValidator orderOwnershipValidator = new OrderOwnershipValidator(EMAIL_NORMALIZER);
        CacheManager cacheManager = mock(CacheManager.class);
        List<Order> savedOrders = java.util.Collections.synchronizedList(new ArrayList<>());

        when(productRepo.findByIdForUpdate(1L)).thenAnswer(invocation -> {
            rowLock.lock();
            return Optional.of(product);
        });

        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            unlockIfHeld(rowLock);
            Order order = invocation.getArgument(0);
            synchronized (savedOrders) {
                order.setId((long) savedOrders.size() + 1);
                savedOrders.add(order);
            }
            return order;
        });

        OrderService orderService = new OrderService(
                productRepo,
                orderRepo,
                userRepo,
                currentUserProvider,
                orderOwnershipValidator,
                cacheManager,
                EMAIL_NORMALIZER
        );

        int threadCount = 2;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger insufficientStockCount = new AtomicInteger();

        try (var executor = Executors.newFixedThreadPool(threadCount)) {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();

                    try {
                        start.await();
                        orderService.placeOrder(orderRequest(1L, 1));
                        successCount.incrementAndGet();
                    } catch (InsufficientStockException e) {
                        insufficientStockCount.incrementAndGet();
                        unlockIfHeld(rowLock);
                    } catch (Exception e) {
                        unlockIfHeld(rowLock);
                        throw new RuntimeException(e);
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

    private static UserRepo userRepo(String email) {
        UserRepo userRepo = mock(UserRepo.class);
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(user(email)));
        return userRepo;
    }

    private static User user(String email) {
        User user = new User();
        user.setId(1);
        user.setUsername("Neeraj");
        user.setEmail(email);
        user.setPassword("password");
        user.setProvider("LOCAL");
        user.setRole("USER");
        user.setTokenVersion(0);
        return user;
    }

    private static CurrentUserProvider currentUserProvider(String email) {
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        when(currentUserProvider.getAuthenticatedEmail()).thenReturn(email);
        return currentUserProvider;
    }

    private static void unlockIfHeld(ReentrantLock rowLock) {
        if (rowLock.isHeldByCurrentThread()) {
            rowLock.unlock();
        }
    }

    private static OrderRequest orderRequest(Long productId, int quantity) {
        return new OrderRequest(
                "Neeraj Kumar",
                "buyer@example.com",
                "9876543210",
                "123 Main Road, Delhi, India",
                "CASH_ON_DELIVERY",
                List.of(new OrderItemRequest(productId, quantity))
        );
    }

    private static Product product(Long id, String name, int stock) {
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