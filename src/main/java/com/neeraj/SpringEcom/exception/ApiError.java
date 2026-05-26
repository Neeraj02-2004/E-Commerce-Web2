package com.neeraj.SpringEcom.exception;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        String path,
        String requestId,
        int status,
        String code,
        String message
) {
}