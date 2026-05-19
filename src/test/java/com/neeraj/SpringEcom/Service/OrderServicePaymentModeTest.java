package com.neeraj.SpringEcom.Service;

import com.neeraj.SpringEcom.exception.InvalidOrderException;
import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.model.dto.OrderItemRequest;
import com.neeraj.SpringEcom.model.dto.OrderRequest;
import com.neeraj.SpringEcom.model.dto.OrderResponse;
import com.neeraj.SpringEcom.repo.OrderRepo;
import com.neeraj.SpringEcom.repo.ProductRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderServicePaymentModeTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void placeOrder_withCashOnDelivery_shouldSetPendingPaymentStatus() {
        ProductRepo productRepo = mock(ProductRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);

        Product product = product(1, "Phone", 5);

        when(productRepo.findByIdForUpdate(1)).thenReturn(Optional.of(product));
        when(productRepo.saveAll(any())).thenReturn(List.of(product));
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        authenticate("buyer@example.com");

        OrderService orderService = new OrderService(productRepo, orderRepo);

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

        Product product = product(1, "Phone", 5);

        when(productRepo.findByIdForUpdate(1)).thenReturn(Optional.of(product));
        when(productRepo.saveAll(any())).thenReturn(List.of(product));
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        authenticate("buyer@example.com");

        OrderService orderService = new OrderService(productRepo, orderRepo);

        OrderResponse response = orderService.placeOrder(orderRequest("COD"));

        assertThat(response.paymentMode()).isEqualTo(AppConstants.PaymentMode.CASH_ON_DELIVERY);
        assertThat(response.paymentStatus()).isEqualTo(AppConstants.PaymentStatus.PENDING);
    }

    @Test
    void placeOrder_withOnlinePayment_shouldSetOnlineAndPending() {
        ProductRepo productRepo = mock(ProductRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);

        Product product = product(1, "Phone", 5);

        when(productRepo.findByIdForUpdate(1)).thenReturn(Optional.of(product));
        when(productRepo.saveAll(any())).thenReturn(List.of(product));
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        authenticate("buyer@example.com");

        OrderService orderService = new OrderService(productRepo, orderRepo);

        OrderResponse response = orderService.placeOrder(orderRequest("ONLINE"));

        assertThat(response.paymentMode()).isEqualTo(AppConstants.PaymentMode.ONLINE);
        assertThat(response.paymentStatus()).isEqualTo(AppConstants.PaymentStatus.PENDING);
    }

    @Test
    void placeOrder_withInvalidPaymentMode_shouldThrow() {
        ProductRepo productRepo = mock(ProductRepo.class);
        OrderRepo orderRepo = mock(OrderRepo.class);

        authenticate("buyer@example.com");

        OrderService orderService = new OrderService(productRepo, orderRepo);

        assertThatThrownBy(() -> orderService.placeOrder(orderRequest("CARD")))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Invalid payment mode");
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of())
        );
    }

    private static OrderRequest orderRequest(String paymentMode) {
        return new OrderRequest(
                "Neeraj Kumar",
                "buyer@example.com",
                "9876543210",
                "123 Main Road, Delhi, India",
                paymentMode,
                List.of(new OrderItemRequest(1, 1))
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