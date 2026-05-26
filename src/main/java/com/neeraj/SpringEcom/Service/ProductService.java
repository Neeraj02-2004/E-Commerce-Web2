package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.exception.FileStorageException;
import com.neeraj.SpringEcom.exception.InvalidProductDataException;
import com.neeraj.SpringEcom.exception.ProductNotFoundException;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.repo.ProductRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final ProductRepo productRepo;
    private final Path imageUploadDir;

    public ProductService(
            ProductRepo productRepo,
            @Value("${app.upload.product-images-dir:uploads/products}") String productImagesDir
    ) {
        this.productRepo = productRepo;
        this.imageUploadDir = Paths.get(productImagesDir).toAbsolutePath().normalize();
    }

    @Cacheable(value = "products")
    public List<Product> getAllProducts() {
        logger.info("Fetching all products from DB");
        return productRepo.findAll();
    }

    @Cacheable(value = "product", key = "#id", unless = "#result == null")
    public Optional<Product> getProductById(Long id) {
        logger.info("Fetching product id: {}", id);
        return productRepo.findById(id);
    }

    @Transactional
    @Caching(
            put = @CachePut(value = "product", key = "#result.id"),
            evict = {
                    @CacheEvict(value = "products", allEntries = true)
            }
    )
    public Product addProduct(Product product, MultipartFile imageFile) {
        logger.info(
                "Adding product name: {}, brand: {}",
                safeLogValue(product.getName()),
                safeLogValue(product.getBrand())
        );

        validateImageFile(imageFile, true);

        Path newImagePath = applyImage(product, imageFile);

        if (newImagePath != null) {
            deleteFileOnRollback(newImagePath);
        }

        Product saved = productRepo.save(product);

        logger.info(
                "Added product id: {}, name: {}, brand: {}",
                saved.getId(),
                safeLogValue(saved.getName()),
                safeLogValue(saved.getBrand())
        );

        return saved;
    }

    @Transactional
    @Caching(
            put = @CachePut(value = "product", key = "#result.id"),
            evict = {
                    @CacheEvict(value = "products", allEntries = true)
            }
    )
    public Product updateProduct(Long id, Product product, MultipartFile imageFile) {
        logger.info(
                "Updating product id: {}, newName: {}, newBrand: {}",
                id,
                safeLogValue(product.getName()),
                safeLogValue(product.getBrand())
        );

        Product existingProduct = productRepo.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        validateImageFile(imageFile, false);

        String oldImageUrl = existingProduct.getImageUrl();

        existingProduct.setName(product.getName());
        existingProduct.setBrand(product.getBrand());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setReleaseDate(product.getReleaseDate());
        existingProduct.setProductAvailable(product.isProductAvailable());
        existingProduct.setStockQuantity(product.getStockQuantity());

        Path newImagePath = applyImage(existingProduct, imageFile);

        if (newImagePath != null) {
            deleteFileOnRollback(newImagePath);
            deleteOldImageAfterCommit(oldImageUrl, existingProduct.getImageUrl());
        }

        Product saved = productRepo.save(existingProduct);

        logger.info(
                "Updated product id: {}, name: {}, brand: {}",
                saved.getId(),
                safeLogValue(saved.getName()),
                safeLogValue(saved.getBrand())
        );

        return saved;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "products", allEntries = true)
    })
    public void deleteProduct(Long id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        logger.info(
                "Disabling product id: {}, name: {}, brand: {}",
                product.getId(),
                safeLogValue(product.getName()),
                safeLogValue(product.getBrand())
        );

        product.setProductAvailable(false);
        product.setStockQuantity(0);

        productRepo.save(product);

        logger.info(
                "Disabled product id: {}, name: {}, brand: {}",
                product.getId(),
                safeLogValue(product.getName()),
                safeLogValue(product.getBrand())
        );
    }

    public List<Product> searchProducts(String keyword) {
        String cleanKeyword = normalizeSearchKeyword(keyword);

        logger.info("Searching keyword: {}", cleanKeyword);
        return productRepo.searchProducts(cleanKeyword);
    }

    public ProductImageResource loadProductImage(String filename) {
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

    private String normalizeSearchKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new InvalidProductDataException("Search keyword is required");
        }

        String cleanKeyword = keyword.trim();

        if (cleanKeyword.length() > 100) {
            throw new InvalidProductDataException("Search keyword must not exceed 100 characters");
        }

        return cleanKeyword;
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

    private void validateImageFile(MultipartFile imageFile, boolean required) {
        if (imageFile == null || imageFile.isEmpty()) {
            if (required) {
                throw new InvalidProductDataException("Product image is required");
            }
            return;
        }

        if (imageFile.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new InvalidProductDataException("Product image must not exceed 5MB");
        }

        String contentType = imageFile.getContentType();

        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidProductDataException("Product image must be JPG, PNG, or WEBP");
        }

        String detectedContentType = detectImageContentType(imageFile);

        if (!contentType.equalsIgnoreCase(detectedContentType)) {
            throw new InvalidProductDataException("Product image content does not match its file type");
        }

        String originalFilename = StringUtils.cleanPath(imageFile.getOriginalFilename());

        if (originalFilename.isBlank() || originalFilename.contains("..")) {
            throw new InvalidProductDataException("Invalid image filename");
        }
    }

    private Path applyImage(Product product, MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }

        Path targetPath = null;

        try {
            Files.createDirectories(imageUploadDir);

            String originalFilename = StringUtils.cleanPath(imageFile.getOriginalFilename());
            String detectedContentType = detectImageContentType(imageFile);
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

            product.setImageName(originalFilename);
            product.setImageType(detectedContentType);
            product.setImageUrl("/api/product-images/" + storedFilename);

            return targetPath;

        } catch (IOException e) {
            deleteFileQuietly(targetPath);
            throw new FileStorageException("Image upload failed", e);
        }
    }

    private String detectImageContentType(MultipartFile imageFile) {
        try (InputStream inputStream = imageFile.getInputStream()) {
            byte[] header = inputStream.readNBytes(12);

            if (isJpeg(header)) {
                return "image/jpeg";
            }

            if (isPng(header)) {
                return "image/png";
            }

            if (isWebp(header)) {
                return "image/webp";
            }

            throw new InvalidProductDataException("Product image must be JPG, PNG, or WEBP");
        } catch (IOException e) {
            throw new InvalidProductDataException("Unable to read product image");
        }
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] header) {
        return header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A;
    }

    private boolean isWebp(byte[] header) {
        return header.length >= 12
                && header[0] == 0x52
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x46
                && header[8] == 0x57
                && header[9] == 0x45
                && header[10] == 0x42
                && header[11] == 0x50;
    }

    private void deleteFileOnRollback(Path path) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteFileQuietly(path);
                }
            }
        });
    }

    private void deleteOldImageAfterCommit(String oldImageUrl, String newImageUrl) {
        if (oldImageUrl == null || oldImageUrl.isBlank() || oldImageUrl.equals(newImageUrl)) {
            return;
        }

        Path oldImagePath = resolveImagePath(oldImageUrl);

        if (oldImagePath == null) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteFileQuietly(oldImagePath);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteFileQuietly(oldImagePath);
            }
        });
    }

    private Path resolveImagePath(String imageUrl) {
        String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        Path imagePath = imageUploadDir.resolve(filename).normalize();

        if (!imagePath.startsWith(imageUploadDir)) {
            return null;
        }

        return imagePath;
    }

    private void deleteFileQuietly(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.warn("Failed to delete image file: {}", path, e);
        }
    }

    private String safeLogValue(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }

        return value.trim();
    }

    public record ProductImageResource(
            Resource resource,
            String contentType,
            String filename
    ) {
    }
}