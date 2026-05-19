package com.neeraj.SpringEcom.model.dto;

import java.math.BigDecimal;
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
        String gatewayRefundId,
        BigDecimal refundAmount,
        LocalDateTime approvedAt,
        LocalDateTime completedAt,
        LocalDateTime refundProcessedAt,
        String refundFailureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}