package com.neeraj.SpringEcom.model.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentVerifyRequest(
        @NotBlank String orderId,
        @NotBlank String razorpayOrderId,
        @NotBlank String razorpayPaymentId,
        @NotBlank String razorpaySignature
) {
}