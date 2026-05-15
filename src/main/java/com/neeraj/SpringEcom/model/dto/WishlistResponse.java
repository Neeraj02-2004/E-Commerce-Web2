package com.neeraj.SpringEcom.model.dto;

import java.math.BigDecimal;

public record WishlistResponse(
        int id,
        String name,
        String brand,
        String description,
        BigDecimal price,
        String category,
        String imageUrl,
        boolean productAvailable,
        int stockQuantity
) {
}
