package com.neeraj.SpringEcom.Service;

import com.neeraj.SpringEcom.exception.OrderAlreadyCancelledException;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.model.OrderItem;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.model.dto.OrderResponse;
import com.neeraj.SpringEcom.repo.OrderRepo;
import com.neeraj.SpringEcom.repo.ProductRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class OrderServiceCancelOrderTest {

    private final ProductRepo productRepo = mock(ProductRepo.class);
    private final OrderRepo orderRepo = mock(OrderRepo.class);
    private final OrderService orderService = new OrderService(productRepo, orderRepo);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cancelOrder_whenOwnerCancelsPlacedOrder_shouldRestoreStockAndReturnCancelledOrder() {
        authenticateAs("buyer@example.com");

        Product product = product(1, "Keyboard", 3);
        Order order = order("ORD123", "buyer@example.com", "PLACED", product, 2);

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));
        when(productRepo.findByIdForUpdate(1)).thenReturn(Optional.of(product));
        when(productRepo.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder("ORD123");

        assertThat(response.orderId()).isEqualTo("ORD123");
        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(product.getStockQuantity()).isEqualTo(5);
        assertThat(order.getStatus()).isEqualTo("CANCELLED");

        verify(productRepo).saveAll(List.of(product));
        verify(orderRepo).save(order);
    }

    @Test
    void cancelOrder_whenDifferentUser_shouldThrowAccessDeniedAndNotRestoreStock() {
        authenticateAs("other@example.com");

        Product product = product(1, "Keyboard", 3);
        Order order = order("ORD123", "buyer@example.com", "PLACED", product, 2);

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("ORD123"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You cannot cancel this order");

        assertThat(product.getStockQuantity()).isEqualTo(3);
        assertThat(order.getStatus()).isEqualTo("PLACED");

        verify(productRepo, never()).findByIdForUpdate(anyInt());
        verify(productRepo, never()).saveAll(any());
        verify(orderRepo, never()).save(any());
    }

    @Test
    void cancelOrder_whenOrderAlreadyCancelled_shouldThrowAndNotRestoreStockAgain() {
        authenticateAs("buyer@example.com");

        Product product = product(1, "Keyboard", 3);
        Order order = order("ORD123", "buyer@example.com", "CANCELLED", product, 2);

        when(orderRepo.findByOrderId("ORD123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("ORD123"))
                .isInstanceOf(OrderAlreadyCancelledException.class);

        assertThat(product.getStockQuantity()).isEqualTo(3);

        verify(productRepo, never()).findByIdForUpdate(anyInt());
        verify(productRepo, never()).saveAll(any());
        verify(orderRepo, never()).save(any());
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of())
        );
    }

    private Product product(int id, String name, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(BigDecimal.valueOf(100));
        product.setStockQuantity(stock);
        return product;
    }

    private Order order(String orderId, String userEmail, String status, Product product, int quantity) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setCustomerName("Neeraj Kumar");
        order.setEmail("buyer@example.com");
        order.setMobileNo("9876543210");
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
