package com.neeraj.SpringEcom.model.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank(message = "Product name is required")
        @Size(min = 2, max = 120, message = "Product name must be between 2 and 120 characters")
        String name,

        @NotBlank(message = "Brand is required")
        @Size(max = 80, message = "Brand must not exceed 80 characters")
        String brand,

        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
        String description,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than 0")
        BigDecimal price,

        @NotBlank(message = "Category is required")
        @Size(max = 80, message = "Category must not exceed 80 characters")
        String category,

        boolean productAvailable,

        @Min(value = 0, message = "Stock quantity cannot be negative")
        int stockQuantity
) {
}
