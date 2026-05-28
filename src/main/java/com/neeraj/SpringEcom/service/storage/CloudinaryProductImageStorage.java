package com.neeraj.SpringEcom.service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.neeraj.SpringEcom.exception.FileStorageException;
import com.neeraj.SpringEcom.exception.ProductNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "cloudinary")
public class CloudinaryProductImageStorage implements ProductImageStorage {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryProductImageStorage.class);

    private final Cloudinary cloudinary;
    private final String folder;

    public CloudinaryProductImageStorage(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret,
            @Value("${cloudinary.folder:springecom/products}") String folder
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
        this.folder = folder;
    }

    @Override
    public StoredProductImage store(MultipartFile imageFile, String detectedContentType) {
        String originalFilename = StringUtils.cleanPath(imageFile.getOriginalFilename());
        String publicId = folder + "/" + UUID.randomUUID();
        long start = System.currentTimeMillis();

        try {
            Map uploadResult = cloudinary.uploader().upload(
                    imageFile.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "public_id", publicId,
                            "overwrite", false
                    )
            );

            Object secureUrl = uploadResult.get("secure_url");

            if (secureUrl == null) {
                throw new FileStorageException("Cloudinary upload did not return image URL");
            }

            return new StoredProductImage(
                    originalFilename,
                    detectedContentType,
                    secureUrl.toString(),
                    publicId
            );

        } catch (IOException e) {
            throw new FileStorageException("Cloudinary image upload failed", e);
        } finally {
            log.info(
                    "Cloudinary image upload took {} ms for file {}",
                    elapsedMillis(start),
                    originalFilename
            );
        }
    }

    @Override
    public ProductImageResource load(String filename) {
        throw new ProductNotFoundException("Product image is served by Cloudinary URL");
    }

    @Override
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        long start = System.currentTimeMillis();

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException e) {
            throw new FileStorageException("Cloudinary image delete failed", e);
        } finally {
            log.info("Cloudinary image delete took {} ms for public id {}", elapsedMillis(start), publicId);
        }
    }

    @Override
    public boolean supportsLocalServing() {
        return false;
    }

    private long elapsedMillis(long start) {
        return System.currentTimeMillis() - start;
    }
}
