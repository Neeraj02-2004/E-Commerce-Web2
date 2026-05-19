package com.neeraj.SpringEcom.model.dto;

import java.time.LocalDateTime;

public record ReturnExchangeResponse(
        String requestId,
        String orderId,
        String userEmail,
        String requestType,
        String reason,
        String status,
        String refundStatus,
        String adminNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}