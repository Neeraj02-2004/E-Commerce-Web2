//package com.neeraj.SpringEcom.Service;
//
//import com.neeraj.SpringEcom.exception.FileStorageException;
//import com.neeraj.SpringEcom.exception.ProductNotFoundException;
//import com.neeraj.SpringEcom.model.Product;
//import com.neeraj.SpringEcom.repo.ProductRepo;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.cache.annotation.CachePut;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.cache.annotation.Caching;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.util.StringUtils;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//@Service
//public class ProductService {
//
//    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
//
//    private final ProductRepo productRepo;
//    private final Path imageUploadDir;
//
//    public ProductService(
//            ProductRepo productRepo,
//            @Value("${app.upload.product-images-dir:uploads/products}") String productImagesDir
//    ) {
//        this.productRepo = productRepo;
//        this.imageUploadDir = Paths.get(productImagesDir).toAbsolutePath().normalize();
//    }
//
//    @Cacheable(value = "products")
//    public List<Product> getAllProducts() {
//        logger.info("Fetching all products from DB");
//        return productRepo.findAll();
//    }
//
//    @Cacheable(value = "product", key = "#id", unless = "#result == null")
//    public Optional<Product> getProductById(int id) {
//        logger.info("Fetching product id: {}", id);
//        return productRepo.findById(id);
//    }
//
//    @Transactional
//    @Caching(
//            put = {
//                    @CachePut(value = "product", key = "#result.id")
//            },
//            evict = {
//                    @CacheEvict(value = "products", allEntries = true),
//                    @CacheEvict(value = "searchProducts", allEntries = true)
//            }
//    )
//    public Product addProduct(Product product, MultipartFile imageFile) {
//        logger.info("Adding new product");
//
//        applyImage(product, imageFile);
//
//        Product saved = productRepo.save(product);
//
//        logger.info("Added product id: {}", saved.getId());
//        return saved;
//    }
//
//    @Transactional
//    @Caching(
//            put = {
//                    @CachePut(value = "product", key = "#result.id")
//            },
//            evict = {
//                    @CacheEvict(value = "product", key = "#id"),
//                    @CacheEvict(value = "products", allEntries = true),
//                    @CacheEvict(value = "searchProducts", allEntries = true)
//            }
//    )
//    public Product updateProduct(int id, Product product, MultipartFile imageFile) {
//        logger.info("Updating product id: {}", id);
//
//        Product existingProduct = productRepo.findById(id)
//                .orElseThrow(() -> new ProductNotFoundException(id));
//
//        existingProduct.setName(product.getName());
//        existingProduct.setBrand(product.getBrand());
//        existingProduct.setDescription(product.getDescription());
//        existingProduct.setPrice(product.getPrice());
//        existingProduct.setCategory(product.getCategory());
//        existingProduct.setReleaseDate(product.getReleaseDate());
//        existingProduct.setProductAvailable(product.isProductAvailable());
//        existingProduct.setStockQuantity(product.getStockQuantity());
//
//        applyImage(existingProduct, imageFile);
//
//        Product saved = productRepo.save(existingProduct);
//
//        logger.info("Updated product id: {}", saved.getId());
//        return saved;
//    }
//
//    private void applyImage(Product product, MultipartFile imageFile) {
//        if (imageFile == null || imageFile.isEmpty()) {
//            return;
//        }
//
//        try {
//            Files.createDirectories(imageUploadDir);
//
//            String originalFilename = StringUtils.cleanPath(imageFile.getOriginalFilename());
//
//            if (originalFilename.contains("..")) {
//                throw new FileStorageException("Invalid image filename");
//            }
//
//            String extension = "";
//            int dotIndex = originalFilename.lastIndexOf(".");
//
//            if (dotIndex >= 0) {
//                extension = originalFilename.substring(dotIndex);
//            }
//
//            String storedFilename = UUID.randomUUID() + extension;
//            Path targetPath = imageUploadDir.resolve(storedFilename).normalize();
//
//            if (!targetPath.startsWith(imageUploadDir)) {
//                throw new FileStorageException("Invalid image path");
//            }
//
//            Files.copy(imageFile.getInputStream(), targetPath);
//
//            product.setImageName(originalFilename);
//            product.setImageType(imageFile.getContentType());
//            product.setImageUrl("/api/product-images/" + storedFilename);
//
//        } catch (IOException e) {
//            throw new FileStorageException("Image upload failed", e);
//        }
//    }
//
//    @Transactional
//    @Caching(evict = {
//            @CacheEvict(value = "product", key = "#id"),
//            @CacheEvict(value = "products", allEntries = true),
//            @CacheEvict(value = "searchProducts", allEntries = true)
//    })
//    public void deleteProduct(int id) {
//        logger.info("Deleting product id: {}", id);
//
//        if (!productRepo.existsById(id)) {
//            throw new ProductNotFoundException(id);
//        }
//
//        productRepo.deleteById(id);
//
//        logger.info("Deleted product id: {}", id);
//    }
//
//    @Cacheable(value = "searchProducts", key = "#keyword")
//    public List<Product> searchProducts(String keyword) {
//        logger.info("Searching keyword: {}", keyword);
//        return productRepo.searchProducts(keyword);
//    }
//}


package com.neeraj.SpringEcom.Service;

import com.neeraj.SpringEcom.exception.FileStorageException;
import com.neeraj.SpringEcom.exception.InvalidProductDataException;
import com.neeraj.SpringEcom.exception.ProductNotFoundException;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.repo.ProductRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    public Optional<Product> getProductById(int id) {
        logger.info("Fetching product id: {}", id);
        return productRepo.findById(id);
    }

    @Transactional
    @Caching(
            put = @CachePut(value = "product", key = "#result.id"),
            evict = {
                    @CacheEvict(value = "products", allEntries = true),
                    @CacheEvict(value = "searchProducts", allEntries = true)
            }
    )
    public Product addProduct(Product product, MultipartFile imageFile) {
        logger.info("Adding new product");

        validateImageFile(imageFile, true);

        Path newImagePath = applyImage(product, imageFile);

        if (newImagePath != null) {
            deleteFileOnRollback(newImagePath);
        }

        Product saved = productRepo.save(product);

        logger.info("Added product id: {}", saved.getId());
        return saved;
    }

    @Transactional
    @Caching(
            put = @CachePut(value = "product", key = "#result.id"),
            evict = {
                    @CacheEvict(value = "products", allEntries = true),
                    @CacheEvict(value = "searchProducts", allEntries = true)
            }
    )
    public Product updateProduct(int id, Product product, MultipartFile imageFile) {
        logger.info("Updating product id: {}", id);

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

        logger.info("Updated product id: {}", saved.getId());
        return saved;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "searchProducts", allEntries = true)
    })
    public void deleteProduct(int id) {
        logger.info("Disabling product id: {}", id);

        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setProductAvailable(false);
        product.setStockQuantity(0);

        productRepo.save(product);

        logger.info("Disabled product id: {}", id);
    }


    @Cacheable(value = "searchProducts", key = "#keyword")
    public List<Product> searchProducts(String keyword) {
        logger.info("Searching keyword: {}", keyword);
        return productRepo.searchProducts(keyword);
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
            product.setImageType(imageFile.getContentType());
            product.setImageUrl("/api/product-images/" + storedFilename);

            return targetPath;

        } catch (IOException e) {
            deleteFileQuietly(targetPath);
            throw new FileStorageException("Image upload failed", e);
        }
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
}
