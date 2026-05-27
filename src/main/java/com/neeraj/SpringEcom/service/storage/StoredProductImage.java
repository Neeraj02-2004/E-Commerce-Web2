package com.neeraj.SpringEcom.service.storage;

public record StoredProductImage(
        String originalFilename,
        String contentType,
        String imageUrl,
        String publicId
) {
}