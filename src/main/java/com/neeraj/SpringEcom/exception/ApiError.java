package com.neeraj.SpringEcom.exception;

public record ApiError(
        String code,
        String message
) {
}
