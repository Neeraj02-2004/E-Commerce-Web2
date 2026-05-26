package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.exception.OrderAlreadyCancelledException;
import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.model.OrderItem;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.model.dto.OrderResponse;
import com.neeraj.SpringEcom.repo.OrderRepo;
import com.neeraj.SpringEcom.repo.ProductRepo;
import com.neeraj.SpringEcom.repo.UserRepo;
import com.neeraj.SpringEcom.security.CurrentUserProvider;
import com.neeraj.SpringEcom.security.OrderOwnershipValidator;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.AccessDeniedException;
import com.neeraj.SpringEcom.util.EmailNormalizer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;


class OrderServiceCancelOrderTest {

    private final ProductRepo productRepo = mock(ProductRepo.class);
    private final OrderRepo orderRepo = mock(OrderRepo.class);
    private final UserRepo userRepo = mock(UserRepo.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final EmailNormalizer emailNormalizer = new EmailNormalizer();
    private final OrderOwnershipValidator orderOwnershipValidator = new OrderOwnershipValidator(emailNormalizer);
    private final CacheManager cacheManager = mock(CacheManager.class);

    private final OrderService orderService = new OrderService(
            productRepo,
            orderRepo,
            userRepo,
            currentUserProvider,
            orderOwnershipValidator,
            cacheManager,
            emailNormalizer
    );

    @Test
    void cancelOrder_whenOwnerCancelsPlacedOrder_shouldRestoreStockAndReturnCancelledOrder() {
        when(currentUserProvider.getAuthenticatedEmail()).thenReturn("buyer@example.com");

        Product product = product(1L, "Keyboard", 3);
        Order order = order("ORD123", "buyer@example.com", AppConstants.OrderStatus.PLACED, product, 2);

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));
        when(productRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder("ORD123");

        assertThat(response.orderId()).isEqualTo("ORD123");
        assertThat(response.status()).isEqualTo(AppConstants.OrderStatus.CANCELLED);
        assertThat(product.getStockQuantity()).isEqualTo(5);
        assertThat(order.getStatus()).isEqualTo(AppConstants.OrderStatus.CANCELLED);

        verify(orderRepo).save(order);
    }

    @Test
    void cancelOrder_whenDifferentUser_shouldThrowAccessDeniedAndNotRestoreStock() {
        when(currentUserProvider.getAuthenticatedEmail()).thenReturn("other@example.com");

        Product product = product(1L, "Keyboard", 3);
        Order order = order("ORD123", "buyer@example.com", AppConstants.OrderStatus.PLACED, product, 2);

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("ORD123"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You cannot cancel this order");

        assertThat(product.getStockQuantity()).isEqualTo(3);
        assertThat(order.getStatus()).isEqualTo(AppConstants.OrderStatus.PLACED);

        verify(productRepo, never()).findByIdForUpdate(anyLong());
        verify(orderRepo, never()).save(any());
    }

    @Test
    void cancelOrder_whenOrderAlreadyCancelled_shouldThrowAndNotRestoreStockAgain() {
        when(currentUserProvider.getAuthenticatedEmail()).thenReturn("buyer@example.com");

        Product product = product(1L, "Keyboard", 3);
        Order order = order("ORD123", "buyer@example.com", AppConstants.OrderStatus.CANCELLED, product, 2);

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("ORD123"))
                .isInstanceOf(OrderAlreadyCancelledException.class);

        assertThat(product.getStockQuantity()).isEqualTo(3);

        verify(productRepo, never()).findByIdForUpdate(anyLong());
        verify(orderRepo, never()).save(any());
    }

    private Product product(Long id, String name, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(BigDecimal.valueOf(100));
        product.setStockQuantity(stock);
        return product;
    }

    private Order order(
            String orderId,
            String userEmail,
            AppConstants.OrderStatus status,
            Product product,
            int quantity
    ) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setCustomerName("Neeraj Kumar");
        order.setEmail("buyer@example.com");
        order.setMobileNo("9876543210");
        order.setAddress("123 Main Road, Delhi, India");
        order.setPaymentMode(AppConstants.PaymentMode.CASH_ON_DELIVERY);
        order.setPaymentStatus(AppConstants.PaymentStatus.PENDING);
        order.setStatus(status);
        order.setOrderDate(LocalDate.now());
        order.setUserEmail(userEmail);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)));

        order.setOrderItems(List.of(item));
        return order;
    }
}