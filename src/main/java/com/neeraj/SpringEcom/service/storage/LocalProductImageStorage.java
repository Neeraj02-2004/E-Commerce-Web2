package com.neeraj.SpringEcom.service.storage;

import com.neeraj.SpringEcom.exception.FileStorageException;
import com.neeraj.SpringEcom.exception.InvalidProductDataException;
import com.neeraj.SpringEcom.exception.ProductNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalProductImageStorage implements ProductImageStorage {

    private final Path imageUploadDir;

    public LocalProductImageStorage(
            @Value("${app.upload.product-images-dir:uploads/products}") String productImagesDir
    ) {
        this.imageUploadDir = Paths.get(productImagesDir).toAbsolutePath().normalize();
    }

    @Override
    public StoredProductImage store(MultipartFile imageFile, String detectedContentType) {
        Path targetPath = null;

        try {
            Files.createDirectories(imageUploadDir);

            String originalFilename = StringUtils.cleanPath(imageFile.getOriginalFilename());
            String extension = "";

            int dotIndex = originalFilename.lastIndexOf(".");
            if (dotIndex >= 0) {
                extension = originalFilename.substring(dotIndex);
            }

            String storedFilename = UUID.randomUUID() + extension;
            targetPath = imageUploadDir.resolve(storedFilename).normalize();

            if (!targetPath.startsWith(imageUploadDir)) {
                throw new FileStorageException("Invalid image path");
            }

            Files.copy(imageFile.getInputStream(), targetPath);

            return new StoredProductImage(
                    originalFilename,
                    detectedContentType,
                    "/api/product-images/" + storedFilename,
                    storedFilename
            );

        } catch (IOException e) {
            deleteFileQuietly(targetPath);
            throw new FileStorageException("Image upload failed", e);
        }
    }

    @Override
    public ProductImageResource load(String filename) {
        String cleanFilename = validateImageFilename(filename);

        try {
            Path imagePath = imageUploadDir.resolve(cleanFilename).normalize();

            if (!imagePath.startsWith(imageUploadDir)
                    || !Files.exists(imagePath)
                    || !Files.isRegularFile(imagePath)) {
                throw new ProductNotFoundException("Product image not found");
            }

            String contentType = Files.probeContentType(imagePath);

            if (contentType == null || !contentType.startsWith("image/")) {
                throw new InvalidProductDataException("Invalid image file type");
            }

            Resource resource = new UrlResource(imagePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ProductNotFoundException("Product image not found");
            }

            return new ProductImageResource(
                    resource,
                    contentType,
                    imagePath.getFileName().toString()
            );

        } catch (MalformedURLException e) {
            throw new ProductNotFoundException("Product image not found");
        } catch (IOException e) {
            throw new ProductNotFoundException("Product image not found");
        }
    }

    @Override
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        Path imagePath = imageUploadDir.resolve(publicId).normalize();

        if (!imagePath.startsWith(imageUploadDir)) {
            return;
        }

        deleteFileQuietly(imagePath);
    }

    @Override
    public boolean supportsLocalServing() {
        return true;
    }

    private String validateImageFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new InvalidProductDataException("Invalid image filename");
        }

        String cleanFilename = StringUtils.cleanPath(filename);

        if (cleanFilename.contains("/") || cleanFilename.contains("\\") || cleanFilename.contains("..")) {
            throw new InvalidProductDataException("Invalid image filename");
        }

        return cleanFilename;
    }

    private void deleteFileQuietly(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}