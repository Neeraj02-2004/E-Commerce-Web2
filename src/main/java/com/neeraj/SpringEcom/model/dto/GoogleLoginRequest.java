package com.neeraj.SpringEcom.model.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(

        @NotBlank(message = "Google token is required")
        String idToken
) {
}
