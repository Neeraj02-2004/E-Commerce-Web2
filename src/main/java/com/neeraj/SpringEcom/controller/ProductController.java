package com.neeraj.SpringEcom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neeraj.SpringEcom.Service.ProductService;
import com.neeraj.SpringEcom.exception.InvalidProductDataException;
import com.neeraj.SpringEcom.exception.ProductNotFoundException;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.model.dto.ProductRequest;
import com.neeraj.SpringEcom.model.dto.ProductResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Validated
@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final Path imageUploadDir;

    public ProductController(
            ProductService productService,
            ObjectMapper objectMapper,
            Validator validator,
            @Value("${app.upload.product-images-dir:uploads/products}") String productImagesDir
    ) {
        this.productService = productService;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.imageUploadDir = Paths.get(productImagesDir).toAbsolutePath().normalize();
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getProducts() {
        List<ProductResponse> products = productService.getAllProducts()
                .stream()
                .map(this::toProductResponse)
                .toList();

        return ResponseEntity.ok(products);
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<ProductResponse> getProductsById(@PathVariable @Min(1) int id) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return ResponseEntity.ok(toProductResponse(product));
    }

    @GetMapping("/product-images/{filename:.+}")
    public ResponseEntity<Resource> getProductImage(@PathVariable String filename) {
        validateImageFilename(filename);

        try {
            Path imagePath = imageUploadDir.resolve(filename).normalize();

            if (!imagePath.startsWith(imageUploadDir)) {
                throw new InvalidProductDataException("Invalid image filename");
            }

            Resource resource = new UrlResource(imagePath.toUri());

            if (!resource.exists() || !resource.isReadable() || !Files.isRegularFile(imagePath)) {
                throw new ProductNotFoundException("Product image not found");
            }

            String contentType = Files.probeContentType(imagePath);

            if (contentType == null || !contentType.startsWith("image/")) {
                throw new InvalidProductDataException("Invalid image file type");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.inline()
                                    .filename(imagePath.getFileName().toString())
                                    .build()
                                    .toString()
                    )
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                    .body(resource);

        } catch (IOException e) {
            throw new ProductNotFoundException("Product image not found");
        }
    }

    @GetMapping("/products/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @RequestParam("keyword") @NotBlank String keyword
    ) {
        List<ProductResponse> products = productService.searchProducts(keyword.trim())
                .stream()
                .map(this::toProductResponse)
                .toList();

        return ResponseEntity.ok(products);
    }

    @PostMapping("/admin/product")
    public ResponseEntity<ProductResponse> addProduct(
            @RequestPart("product") String productJson,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        try {
            ProductRequest request = objectMapper.readValue(productJson, ProductRequest.class);
            validateProductRequest(request);

            Product savedProduct = productService.addProduct(toProduct(request), imageFile);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toProductResponse(savedProduct));

        } catch (IOException e) {
            throw new InvalidProductDataException("Invalid product data");
        }
    }

    @PutMapping("/admin/product/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable @Min(1) int id,
            @RequestPart("product") String productJson,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        try {
            ProductRequest request = objectMapper.readValue(productJson, ProductRequest.class);
            validateProductRequest(request);

            Product updatedProduct = productService.updateProduct(id, toProduct(request), imageFile);

            return ResponseEntity.ok(toProductResponse(updatedProduct));

        } catch (IOException e) {
            throw new InvalidProductDataException("Invalid product data");
        }
    }

    @DeleteMapping("/admin/product/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable @Min(1) int id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    private void validateProductRequest(ProductRequest request) {
        if (request == null) {
            throw new InvalidProductDataException("Product data is required");
        }

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void validateImageFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new InvalidProductDataException("Invalid image filename");
        }

        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new InvalidProductDataException("Invalid image filename");
        }
    }

    private Product toProduct(ProductRequest request) {
        Product product = new Product();

        product.setName(request.name().trim());
        product.setBrand(request.brand().trim());
        product.setDescription(request.description().trim());
        product.setPrice(request.price());
        product.setCategory(request.category().trim());
        product.setProductAvailable(request.productAvailable());
        product.setStockQuantity(request.stockQuantity());

        return product;
    }

    private ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBrand(),
                product.getPrice(),
                product.getCategory(),
                product.getReleaseDate(),
                product.isProductAvailable(),
                product.getStockQuantity(),
                product.getImageName(),
                product.getImageType(),
                product.getImageUrl()
        );
    }
}
