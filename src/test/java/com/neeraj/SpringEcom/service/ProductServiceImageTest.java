package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.exception.InvalidProductDataException;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.repo.ProductRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceImageTest {

    @TempDir
    Path tempDir;

    private final ProductRepo productRepo = mock(ProductRepo.class);

    @Test
    void addProduct_withValidImage_shouldSaveProductAndStoreImageDetails() throws Exception {
        ProductService productService = new ProductService(productRepo, tempDir.toString());

        Product product = validProduct();

        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile",
                "phone.png",
                "image/png",
                pngBytes()
        );

        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Product savedProduct = productService.addProduct(product, imageFile);

        long storedFileCount = Files.list(tempDir).count();

        assertThat(savedProduct.getId()).isEqualTo(1L);
        assertThat(savedProduct.getImageName()).isEqualTo("phone.png");
        assertThat(savedProduct.getImageType()).isEqualTo("image/png");
        assertThat(savedProduct.getImageUrl()).startsWith("/api/product-images/");
        assertThat(storedFileCount).isEqualTo(1);

        verify(productRepo).save(product);
    }

    @Test
    void addProduct_withSpoofedImageContentType_shouldThrowInvalidProductDataException() {
        ProductService productService = new ProductService(productRepo, tempDir.toString());

        Product product = validProduct();

        MockMultipartFile spoofedImage = new MockMultipartFile(
                "imageFile",
                "phone.png",
                "image/png",
                "not-a-real-image".getBytes()
        );

        assertThatThrownBy(() -> productService.addProduct(product, spoofedImage))
                .isInstanceOf(InvalidProductDataException.class)
                .hasMessage("Product image must be JPG, PNG, or WEBP");

        verify(productRepo, never()).save(any());
    }

    @Test
    void addProduct_withoutImage_shouldThrowInvalidProductDataException() {
        ProductService productService = new ProductService(productRepo, tempDir.toString());

        Product product = validProduct();

        assertThatThrownBy(() -> productService.addProduct(product, null))
                .isInstanceOf(InvalidProductDataException.class)
                .hasMessage("Product image is required");

        verify(productRepo, never()).save(any());
    }

    @Test
    void addProduct_withEmptyImage_shouldThrowInvalidProductDataException() {
        ProductService productService = new ProductService(productRepo, tempDir.toString());

        Product product = validProduct();

        MockMultipartFile emptyImage = new MockMultipartFile(
                "imageFile",
                "phone.png",
                "image/png",
                new byte[0]
        );

        assertThatThrownBy(() -> productService.addProduct(product, emptyImage))
                .isInstanceOf(InvalidProductDataException.class)
                .hasMessage("Product image is required");

        verify(productRepo, never()).save(any());
    }

    @Test
    void addProduct_withInvalidImageType_shouldThrowInvalidProductDataException() {
        ProductService productService = new ProductService(productRepo, tempDir.toString());

        Product product = validProduct();

        MockMultipartFile textFile = new MockMultipartFile(
                "imageFile",
                "notes.txt",
                "text/plain",
                "not-an-image".getBytes()
        );

        assertThatThrownBy(() -> productService.addProduct(product, textFile))
                .isInstanceOf(InvalidProductDataException.class)
                .hasMessage("Product image must be JPG, PNG, or WEBP");

        verify(productRepo, never()).save(any());
    }

    @Test
    void addProduct_withOversizedImage_shouldThrowInvalidProductDataException() {
        ProductService productService = new ProductService(productRepo, tempDir.toString());

        Product product = validProduct();

        byte[] largeContent = new byte[(5 * 1024 * 1024) + 1];

        MockMultipartFile largeImage = new MockMultipartFile(
                "imageFile",
                "phone.png",
                "image/png",
                largeContent
        );

        assertThatThrownBy(() -> productService.addProduct(product, largeImage))
                .isInstanceOf(InvalidProductDataException.class)
                .hasMessage("Product image must not exceed 5MB");

        verify(productRepo, never()).save(any());
    }

    @Test
    void addProduct_withUnsafeFilename_shouldThrowInvalidProductDataException() {
        ProductService productService = new ProductService(productRepo, tempDir.toString());

        Product product = validProduct();

        MockMultipartFile unsafeImage = new MockMultipartFile(
                "imageFile",
                "../phone.png",
                "image/png",
                pngBytes()
        );

        assertThatThrownBy(() -> productService.addProduct(product, unsafeImage))
                .isInstanceOf(InvalidProductDataException.class)
                .hasMessage("Invalid image filename");

        verify(productRepo, never()).save(any());
    }

    private byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D
        };
    }

    private Product validProduct() {
        Product product = new Product();
        product.setName("Smart Phone");
        product.setBrand("Samsung");
        product.setDescription("A reliable Android smartphone");
        product.setPrice(BigDecimal.valueOf(24999));
        product.setCategory("Mobiles");
        product.setProductAvailable(true);
        product.setStockQuantity(10);
        return product;
    }
}