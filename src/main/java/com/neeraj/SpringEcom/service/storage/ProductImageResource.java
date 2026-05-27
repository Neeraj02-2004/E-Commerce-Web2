package com.neeraj.SpringEcom.service.storage;

import org.springframework.core.io.Resource;

public record ProductImageResource(
        Resource resource,
        String contentType,
        String filename
) {
}