package com.neeraj.SpringEcom.model.dto;

public record UserResponse(
        Integer id,
        String username,
        String email,
        String provider,
        String role
) {
}