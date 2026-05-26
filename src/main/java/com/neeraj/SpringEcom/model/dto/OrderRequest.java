package com.neeraj.SpringEcom.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderRequest(

        @NotBlank(message = "Customer name is required")
        @Size(min = 2, max = 80, message = "Customer name must be between 2 and 80 characters")
        String customerName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Mobile number is required")
        @Pattern(
                regexp = "^(?:\\+91|91|0)?[6-9]\\d{9}$",
                message = "Mobile number must be a valid Indian number"
        )
        String mobileNo,

        @NotBlank(message = "Address is required")
        @Size(min = 10, max = 500, message = "Address must be between 10 and 500 characters")
        String address,

        @NotBlank(message = "Payment mode is required")
        @Size(max = 50, message = "Payment mode must not exceed 50 characters")
        String paymentMode,

        @NotEmpty(message = "Order must contain at least one item")
        List<@Valid OrderItemRequest> items
) {
}