package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.exception.InvalidProductDataException;
import com.neeraj.SpringEcom.exception.ProductNotFoundException;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.model.dto.ProductPageResponse;
import com.neeraj.SpringEcom.repo.ProductRepo;
import com.neeraj.SpringEcom.service.storage.LocalProductImageStorage;
import com.neeraj.SpringEcom.service.storage.ProductImageResource;
import com.neeraj.SpringEcom.service.storage.ProductImageStorage;
import com.neeraj.SpringEcom.service.storage.StoredProductImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private final ProductImageStorage productImageStorage;
    private final CacheManager cacheManager;

    @Autowired
    public ProductService(
            ProductRepo productRepo,
            ProductImageStorage productImageStorage,
            CacheManager cacheManager
    ) {
        this.productRepo = productRepo;
        this.productImageStorage = productImageStorage;
        this.cacheManager = cacheManager;
    }

    public ProductService(ProductRepo productRepo, String productImagesDir) {
        this.productRepo = productRepo;
        this.productImageStorage = new LocalProductImageStorage(productImagesDir);
        this.cacheManager = null;
    }

    @Cacheable(value = "products")
    public List<Product> getAllProducts() {
        logger.info("Fetching all products from DB");
        return productRepo.findAll();
    }

    public Page<Product> getProducts(int page, int size) {
        String cacheKey = "page:" + page + ":size:" + size;

        Cache cache = null;

        if (cacheManager != null) {
            cache = cacheManager.getCache("productsPage");
        }

        if (cache != null) {
            ProductPageResponse response = cache.get(cacheKey, ProductPageResponse.class);

            if (response != null) {
                logger.info("Fetching products page from Redis cache: {}, size: {}", page, size);

                Pageable pageable = PageRequest.of(
                        response.getPage(),
                        response.getSize(),
                        Sort.by(Sort.Direction.DESC, "id")
                );

                return new PageImpl<>(
                        response.getContent(),
                        pageable,
                        response.getTotalElements()
                );
            }
        }

        logger.info("Fetching products page from DB: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<Product> productPage = productRepo.findAll(pageable);

        ProductPageResponse productPageResponse = new ProductPageResponse(
                productPage.getContent(),
                page,
                size,
                productPage.getTotalElements()
        );

        if (cache != null) {
            cache.put(cacheKey, productPageResponse);
        }

        return productPage;
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
                    @CacheEvict(value = "products", allEntries = true),
                    @CacheEvict(value = "productsPage", allEntries = true)
            }
    )
    public Product addProduct(Product product, MultipartFile imageFile) {
        logger.info(
                "Adding product name: {}, brand: {}",
                safeLogValue(product.getName()),
                safeLogValue(product.getBrand())
        );

        validateImageFile(imageFile, true);

        StoredProductImage storedImage = applyImage(product, imageFile);

        if (storedImage != null) {
            deleteImageOnRollback(storedImage.publicId());
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
                    @CacheEvict(value = "products", allEntries = true),
                    @CacheEvict(value = "productsPage", allEntries = true)
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

        String oldImagePublicId = existingProduct.getImagePublicId();

        existingProduct.setName(product.getName());
        existingProduct.setBrand(product.getBrand());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setReleaseDate(product.getReleaseDate());
        existingProduct.setProductAvailable(product.isProductAvailable());
        existingProduct.setStockQuantity(product.getStockQuantity());

        StoredProductImage storedImage = applyImage(existingProduct, imageFile);

        if (storedImage != null) {
            deleteImageOnRollback(storedImage.publicId());
            deleteOldImageAfterCommit(oldImagePublicId, existingProduct.getImagePublicId());
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
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "productsPage", allEntries = true)
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
        return productImageStorage.load(filename);
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

    private StoredProductImage applyImage(Product product, MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }

        String detectedContentType = detectImageContentType(imageFile);
        StoredProductImage storedImage = productImageStorage.store(imageFile, detectedContentType);

        product.setImageName(storedImage.originalFilename());
        product.setImageType(storedImage.contentType());
        product.setImageUrl(storedImage.imageUrl());
        product.setImagePublicId(storedImage.publicId());

        return storedImage;
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

    private void deleteImageOnRollback(String publicId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    productImageStorage.delete(publicId);
                }
            }
        });
    }

    private void deleteOldImageAfterCommit(String oldImagePublicId, String newImagePublicId) {
        if (oldImagePublicId == null || oldImagePublicId.isBlank() || oldImagePublicId.equals(newImagePublicId)) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            productImageStorage.delete(oldImagePublicId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                productImageStorage.delete(oldImagePublicId);
            }
        });
    }

    private String safeLogValue(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }

        return value.trim();
    }
}