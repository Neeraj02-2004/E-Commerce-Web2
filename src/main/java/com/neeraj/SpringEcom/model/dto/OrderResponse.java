package com.neeraj.SpringEcom.model.dto;

import com.neeraj.SpringEcom.model.AppConstants;

import java.time.LocalDate;
import java.util.List;

public record OrderResponse(
        String orderId,
        String customerName,
        String email,
        String mobileNo,
        String address,
        AppConstants.PaymentMode paymentMode,
        AppConstants.PaymentStatus paymentStatus,
        String gatewayOrderId,
        String gatewayPaymentId,
        AppConstants.OrderStatus status,
        LocalDate orderDate,
        List<OrderItemResponse> items
) {
}