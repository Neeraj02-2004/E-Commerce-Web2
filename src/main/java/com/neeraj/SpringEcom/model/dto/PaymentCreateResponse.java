package com.neeraj.SpringEcom.model.dto;

public record PaymentCreateResponse(
        String keyId,
        String orderId,
        String razorpayOrderId,
        Long amount,
        String currency,
        String customerName,
        String email,
        String mobileNo
) {
}