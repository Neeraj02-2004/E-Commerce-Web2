package com.neeraj.SpringEcom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neeraj.SpringEcom.exception.InvalidProductDataException;
import com.neeraj.SpringEcom.exception.ProductNotFoundException;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.model.dto.ProductRequest;
import com.neeraj.SpringEcom.model.dto.ProductResponse;
import com.neeraj.SpringEcom.service.ProductService;
import com.neeraj.SpringEcom.service.storage.ProductImageResource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    public ProductController(
            ProductService productService,
            ObjectMapper objectMapper,
            Validator validator
    ) {
        this.productService = productService;
        this.objectMapper = objectMapper;
        this.validator = validator;
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
    public ResponseEntity<ProductResponse> getProductsById(@PathVariable @Min(1) Long id) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return ResponseEntity.ok(toProductResponse(product));
    }

    @GetMapping("/product-images/{filename:.+}")
    public ResponseEntity<Resource> getProductImage(@PathVariable String filename) {
        ProductImageResource image = productService.loadProductImage(filename);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(image.filename())
                                .build()
                                .toString()
                )
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(image.resource());
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
            @PathVariable @Min(1) Long id,
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
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable @Min(1) Long id) {
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

    private Product toProduct(ProductRequest request) {
        Product product = new Product();

        product.setName(request.name().trim());
        product.setBrand(request.brand().trim());
        product.setDescription(request.description().trim());
        product.setPrice(request.price());
        product.setCategory(request.category().trim());
        product.setReleaseDate(request.releaseDate());
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