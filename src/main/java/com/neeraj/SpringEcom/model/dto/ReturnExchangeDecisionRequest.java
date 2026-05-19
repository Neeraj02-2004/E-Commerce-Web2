package com.neeraj.SpringEcom.model.dto;

import jakarta.validation.constraints.Size;

public record ReturnExchangeDecisionRequest(
        @Size(max = 1000, message = "Admin note cannot exceed 1000 characters")
        String adminNote
) {
}