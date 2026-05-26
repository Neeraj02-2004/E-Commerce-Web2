package com.neeraj.SpringEcom.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String brand,
        BigDecimal price,
        String category,
        LocalDate releaseDate,
        boolean productAvailable,
        int stockQuantity,
        String imageName,
        String imageType,
        String imageUrl
) {
}