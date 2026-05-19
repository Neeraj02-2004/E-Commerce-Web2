package com.neeraj.SpringEcom.model.dto;

public record PaymentVerifyResponse(
        String orderId,
        String paymentStatus,
        String message
) {
}