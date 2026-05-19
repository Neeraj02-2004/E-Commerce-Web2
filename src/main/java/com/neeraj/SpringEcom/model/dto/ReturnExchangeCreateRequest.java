package com.neeraj.SpringEcom.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReturnExchangeCreateRequest(
        @NotBlank(message = "Request type is required")
        String requestType,

        @NotBlank(message = "Reason is required")
        @Size(min = 10, max = 1000, message = "Reason must be between 10 and 1000 characters")
        String reason
) {
}