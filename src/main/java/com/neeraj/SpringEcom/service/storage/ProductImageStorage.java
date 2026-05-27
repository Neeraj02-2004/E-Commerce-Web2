package com.neeraj.SpringEcom.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ProductImageStorage {

    StoredProductImage store(MultipartFile imageFile, String detectedContentType);

    ProductImageResource load(String filename);

    void delete(String publicId);

    boolean supportsLocalServing();
}