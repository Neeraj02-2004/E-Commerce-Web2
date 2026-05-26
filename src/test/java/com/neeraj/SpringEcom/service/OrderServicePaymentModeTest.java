package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.exception.InvalidOrderException;
import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.model.User;
import com.neeraj.SpringEcom.model.dto.OrderItemRequest;
import com.neeraj.SpringEcom.model.dto.OrderRequest;
import com.neeraj.SpringEcom.model.dto.OrderResponse;
import com.neeraj.SpringEcom.repo.OrderRepo;
import com.neeraj.SpringEcom.repo.ProductRepo;
import com.neeraj.SpringEcom.repo.UserRepo;
import com.neeraj.SpringEcom.security.CurrentUserProvider;
import com.neeraj.SpringEcom.security.OrderOwnershipValidator;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.neeraj.SpringEcom.util.EmailNormalizer;


class OrderServicePaymentModeTest {

    private static final EmailNormalizer EMAIL_NORMALIZER = new EmailNormalizer();

    @Test
    void placeOrder_withCashOnDelivery_shouldSetPendingPaymentStatus() {
        ProductRepo productRepo = mock(ProductRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        UserRepo userRepo = userRepo();
        CurrentUserProvider currentUserProvider = currentUserProvider();
        OrderOwnershipValidator orderOwnershipValidator = new OrderOwnershipValidator(EMAIL_NORMALIZER);
        CacheManager cacheManager = mock(CacheManager.class);

        Product product = product(1L, "Phone", 5);

        when(productRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
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

        OrderResponse response = orderService.placeOrder(orderRequest("CASH_ON_DELIVERY"));

        assertThat(response.paymentMode()).isEqualTo(AppConstants.PaymentMode.CASH_ON_DELIVERY);
        assertThat(response.paymentStatus()).isEqualTo(AppConstants.PaymentStatus.PENDING);
        assertThat(response.address()).isEqualTo("123 Main Road, Delhi, India");
        assertThat(product.getStockQuantity()).isEqualTo(4);
    }

    @Test
    void placeOrder_withCodAlias_shouldAcceptCashOnDelivery() {
        ProductRepo productRepo = mock(ProductRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        UserRepo userRepo = userRepo();
        CurrentUserProvider currentUserProvider = currentUserProvider();
        OrderOwnershipValidator orderOwnershipValidator = new OrderOwnershipValidator(EMAIL_NORMALIZER);
        CacheManager cacheManager = mock(CacheManager.class);

        Product product = product(1L, "Phone", 5);

        when(productRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
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

        OrderResponse response = orderService.placeOrder(orderRequest("COD"));

        assertThat(response.paymentMode()).isEqualTo(AppConstants.PaymentMode.CASH_ON_DELIVERY);
        assertThat(response.paymentStatus()).isEqualTo(AppConstants.PaymentStatus.PENDING);
    }

    @Test
    void placeOrder_withOnlinePayment_shouldSetOnlineAndPending() {
        ProductRepo productRepo = mock(ProductRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        UserRepo userRepo = userRepo();
        CurrentUserProvider currentUserProvider = currentUserProvider();
        OrderOwnershipValidator orderOwnershipValidator = new OrderOwnershipValidator(EMAIL_NORMALIZER);
        CacheManager cacheManager = mock(CacheManager.class);

        Product product = product(1L, "Phone", 5);

        when(productRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
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

        OrderResponse response = orderService.placeOrder(orderRequest("ONLINE"));

        assertThat(response.paymentMode()).isEqualTo(AppConstants.PaymentMode.ONLINE);
        assertThat(response.paymentStatus()).isEqualTo(AppConstants.PaymentStatus.PENDING);
    }

    @Test
    void placeOrder_withInvalidPaymentMode_shouldThrow() {
        ProductRepo productRepo = mock(ProductRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        UserRepo userRepo = userRepo();
        CurrentUserProvider currentUserProvider = currentUserProvider();
        OrderOwnershipValidator orderOwnershipValidator = new OrderOwnershipValidator(EMAIL_NORMALIZER);
        CacheManager cacheManager = mock(CacheManager.class);

        OrderService orderService = new OrderService(
                productRepo,
                orderRepo,
                userRepo,
                currentUserProvider,
                orderOwnershipValidator,
                cacheManager,
                EMAIL_NORMALIZER
        );

        assertThatThrownBy(() -> orderService.placeOrder(orderRequest("CARD")))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Invalid payment mode");
    }

    private static UserRepo userRepo() {
        UserRepo userRepo = mock(UserRepo.class);
        when(userRepo.findByEmail("buyer@example.com")).thenReturn(Optional.of(user()));
        return userRepo;
    }

    private static User user() {
        User user = new User();
        user.setId(1);
        user.setUsername("Neeraj");
        user.setEmail("buyer@example.com");
        user.setPassword("password");
        user.setProvider("LOCAL");
        user.setRole("USER");
        user.setTokenVersion(0);
        return user;
    }

    private static CurrentUserProvider currentUserProvider() {
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        when(currentUserProvider.getAuthenticatedEmail()).thenReturn("buyer@example.com");
        return currentUserProvider;
    }

    private static OrderRequest orderRequest(String paymentMode) {
        return new OrderRequest(
                "Neeraj Kumar",
                "buyer@example.com",
                "9876543210",
                "123 Main Road, Delhi, India",
                paymentMode,
                List.of(new OrderItemRequest(1L, 1))
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