
package com.neeraj.SpringEcom.Service;

import com.neeraj.SpringEcom.exception.InsufficientStockException;
import com.neeraj.SpringEcom.exception.InvalidOrderException;
import com.neeraj.SpringEcom.exception.OrderAlreadyCancelledException;
import com.neeraj.SpringEcom.exception.OrderNotFoundException;
import com.neeraj.SpringEcom.exception.ProductNotFoundException;
import com.neeraj.SpringEcom.exception.UserNotAuthenticatedException;
import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.Order;
import com.neeraj.SpringEcom.model.OrderItem;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.model.dto.OrderItemRequest;
import com.neeraj.SpringEcom.model.dto.OrderItemResponse;
import com.neeraj.SpringEcom.model.dto.OrderRequest;
import com.neeraj.SpringEcom.model.dto.OrderResponse;
import com.neeraj.SpringEcom.model.dto.PageResponse;
import com.neeraj.SpringEcom.repo.OrderRepo;
import com.neeraj.SpringEcom.repo.ProductRepo;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private final ProductRepo productRepo;
    private final OrderRepo orderRepo;

    public OrderService(ProductRepo productRepo, OrderRepo orderRepo) {
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product", allEntries = true),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "searchProducts", allEntries = true)
    })
    public OrderResponse placeOrder(OrderRequest request) {
        validateOrderRequest(request);

        String userEmail = getAuthenticatedEmail();
        String paymentMode = validatePaymentMode(request.paymentMode());

        Order order = new Order();
        order.setOrderId("ORD" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        order.setCustomerName(validateName(request.customerName()));
        order.setEmail(validateEmail(request.email()));
        order.setMobileNo(normalizeMobile(request.mobileNo()));
        order.setAddress(validateAddress(request.address()));
        order.setPaymentMode(paymentMode);
        order.setPaymentStatus(AppConstants.PaymentStatus.PENDING);
        order.setStatus(AppConstants.OrderStatus.PLACED);
        order.setOrderDate(LocalDate.now());
        order.setUserEmail(userEmail);

        List<OrderItem> orderItems = new ArrayList<>();
        List<Product> updatedProducts = new ArrayList<>();

        for (OrderItemRequest itemReq : request.items()) {
            validateOrderItemRequest(itemReq);

            Product product = productRepo.findByIdForUpdate(itemReq.productId())
                    .orElseThrow(() -> new ProductNotFoundException(itemReq.productId()));

            if (!product.isProductAvailable()) {
                throw new InvalidOrderException("Product is not available: " + product.getName());
            }

            int updatedStock = product.getStockQuantity() - itemReq.quantity();

            if (updatedStock < 0) {
                throw new InsufficientStockException(
                        "Insufficient stock for product: " + product.getName()
                );
            }

            product.setStockQuantity(updatedStock);
            updatedProducts.add(product);

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(itemReq.quantity());
            item.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));
            item.setOrder(order);

            orderItems.add(item);
        }

        productRepo.saveAll(updatedProducts);

        order.setOrderItems(orderItems);

        Order saved = orderRepo.save(order);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrderResponses(int page, int size) {
        String userEmail = getAuthenticatedEmail();

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<Long> orderIdPage = orderRepo.findOrderIdsByUserEmail(userEmail, pageable);

        List<Order> orders = orderIdPage.getContent().isEmpty()
                ? List.of()
                : orderRepo.findAllWithItemsByIdIn(orderIdPage.getContent());

        Map<Long, Order> ordersById = new java.util.HashMap<>();

        for (Order order : orders) {
            ordersById.put(order.getId(), order);
        }

        List<OrderResponse> responses = orderIdPage.getContent()
                .stream()
                .map(ordersById::get)
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(
                responses,
                orderIdPage.getNumber(),
                orderIdPage.getSize(),
                orderIdPage.getTotalPages(),
                orderIdPage.getTotalElements()
        );
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product", allEntries = true),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "searchProducts", allEntries = true)
    })
    public OrderResponse cancelOrder(String orderId) {
        String userEmail = getAuthenticatedEmail();

        Order order = orderRepo.findByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!userEmail.equalsIgnoreCase(order.getUserEmail())) {
            throw new AccessDeniedException("You cannot cancel this order");
        }

        if (AppConstants.OrderStatus.CANCELLED.equals(order.getStatus())) {
            throw new OrderAlreadyCancelledException(orderId);
        }

        if (AppConstants.PaymentStatus.PAID.equals(order.getPaymentStatus())) {
            throw new InvalidOrderException("Paid orders cannot be cancelled from this endpoint");
        }

        List<Product> updatedProducts = new ArrayList<>();

        for (OrderItem item : order.getOrderItems()) {
            Product product = productRepo.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ProductNotFoundException(item.getProduct().getId()));

            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            updatedProducts.add(product);
        }

        productRepo.saveAll(updatedProducts);

        order.setStatus(AppConstants.OrderStatus.CANCELLED);

        Order saved = orderRepo.save(order);

        return toResponse(saved);
    }

    private void validateOrderRequest(OrderRequest request) {
        if (request == null) {
            throw new InvalidOrderException("Order request is required");
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one item");
        }
    }

    private void validateOrderItemRequest(OrderItemRequest itemReq) {
        if (itemReq == null) {
            throw new InvalidOrderException("Order item is required");
        }

        if (itemReq.productId() == null || itemReq.productId() <= 0) {
            throw new InvalidOrderException("Product id must be greater than zero");
        }

        if (itemReq.quantity() == null || itemReq.quantity() <= 0) {
            throw new InvalidOrderException("Quantity must be greater than zero");
        }
    }

    private String getAuthenticatedEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new UserNotAuthenticatedException("User not authenticated");
        }

        return auth.getName().toLowerCase().trim();
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidOrderException("Customer name is required");
        }

        String cleanName = name.trim();

        if (cleanName.length() < 2 || cleanName.length() > 100) {
            throw new InvalidOrderException("Customer name must be between 2 and 100 characters");
        }

        return cleanName;
    }

    private String validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidOrderException("Email is required");
        }

        String cleanEmail = email.trim().toLowerCase();

        if (!cleanEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new InvalidOrderException("Invalid email format");
        }

        return cleanEmail;
    }

    private String normalizeMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            throw new InvalidOrderException("Mobile number is required");
        }

        String cleanMobile = mobile.trim().replaceAll("\\s+", "");

        if (cleanMobile.startsWith("+91") && cleanMobile.length() == 13) {
            return cleanMobile.substring(3);
        }

        if (cleanMobile.startsWith("91") && cleanMobile.length() == 12) {
            return cleanMobile.substring(2);
        }

        if (cleanMobile.startsWith("0") && cleanMobile.length() == 11) {
            return cleanMobile.substring(1);
        }

        if (cleanMobile.length() == 10) {
            return cleanMobile;
        }

        throw new InvalidOrderException("Invalid mobile number format");
    }

    private String validateAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new InvalidOrderException("Address is required");
        }

        String cleanAddress = address.trim().replaceAll("\\s+", " ");

        if (cleanAddress.length() < 10 || cleanAddress.length() > 500) {
            throw new InvalidOrderException("Address must be between 10 and 500 characters");
        }

        return cleanAddress;
    }


    private String validatePaymentMode(String paymentMode) {
        if (paymentMode == null || paymentMode.isBlank()) {
            throw new InvalidOrderException("Payment mode is required");
        }

        String cleanPaymentMode = paymentMode.trim().toUpperCase().replace(" ", "_");

        if ("COD".equals(cleanPaymentMode)) {
            return AppConstants.PaymentMode.CASH_ON_DELIVERY;
        }

        if (AppConstants.PaymentMode.CASH_ON_DELIVERY.equals(cleanPaymentMode)) {
            return AppConstants.PaymentMode.CASH_ON_DELIVERY;
        }

        if ("RAZORPAY".equals(cleanPaymentMode)
                || "ONLINE_PAYMENT".equals(cleanPaymentMode)
                || AppConstants.PaymentMode.ONLINE.equals(cleanPaymentMode)) {
            return AppConstants.PaymentMode.ONLINE;
        }

        throw new InvalidOrderException("Invalid payment mode");
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getTotalPrice()
                ))
                .toList();

        return new OrderResponse(
                order.getOrderId(),
                order.getCustomerName(),
                order.getEmail(),
                order.getMobileNo(),
                order.getAddress(),
                order.getPaymentMode(),
                order.getPaymentStatus(),
                order.getGatewayOrderId(),
                order.getGatewayPaymentId(),
                order.getStatus(),
                order.getOrderDate(),
                itemResponses
        );
    }
}