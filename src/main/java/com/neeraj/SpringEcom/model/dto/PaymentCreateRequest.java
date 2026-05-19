package com.neeraj.SpringEcom.model.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentCreateRequest(
        @NotBlank(message = "Order id is required")
        String orderId
) {
}