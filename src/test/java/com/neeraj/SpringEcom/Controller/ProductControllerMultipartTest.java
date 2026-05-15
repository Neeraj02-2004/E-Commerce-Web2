package com.neeraj.SpringEcom.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neeraj.SpringEcom.Service.JwtService;
import com.neeraj.SpringEcom.Service.MyUserDetailsService;
import com.neeraj.SpringEcom.Service.ProductService;
import com.neeraj.SpringEcom.controller.ProductController;
import com.neeraj.SpringEcom.exception.GlobalExceptionHandler;
import com.neeraj.SpringEcom.exception.InvalidProductDataException;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.model.dto.ProductRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ProductControllerMultipartTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private MyUserDetailsService myUserDetailsService;

    @Test
    void addProduct_withValidProductAndImage_shouldReturnCreatedProduct() throws Exception {
        Product savedProduct = product();
        savedProduct.setId(1);
        savedProduct.setImageName("phone.png");
        savedProduct.setImageType("image/png");
        savedProduct.setImageUrl("/api/product-images/generated-phone.png");

        when(productService.addProduct(any(Product.class), any())).thenReturn(savedProduct);

        mockMvc.perform(multipart("/api/admin/product")
                        .file(productPart(validProductRequest()))
                        .file(imagePart()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Smart Phone"))
                .andExpect(jsonPath("$.brand").value("Samsung"))
                .andExpect(jsonPath("$.imageName").value("phone.png"))
                .andExpect(jsonPath("$.imageType").value("image/png"))
                .andExpect(jsonPath("$.imageUrl").value("/api/product-images/generated-phone.png"));

        verify(productService).addProduct(any(Product.class), any());
    }

    @Test
    void addProduct_withInvalidProductJson_shouldReturnBadRequest() throws Exception {
        MockMultipartFile invalidProductPart = new MockMultipartFile(
                "product",
                "",
                "application/json",
                "{ invalid-json".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/product")
                        .file(invalidProductPart)
                        .file(imagePart()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addProduct_withBlankProductFields_shouldReturnBadRequest() throws Exception {
        ProductRequest invalidRequest = new ProductRequest(
                "",
                "",
                "short",
                BigDecimal.ZERO,
                "",
                true,
                -1
        );

        mockMvc.perform(multipart("/api/admin/product")
                        .file(productPart(invalidRequest))
                        .file(imagePart()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addProduct_whenServiceRejectsMissingImage_shouldReturnBadRequest() throws Exception {
        when(productService.addProduct(any(Product.class), any()))
                .thenThrow(new InvalidProductDataException("Product image is required"));

        mockMvc.perform(multipart("/api/admin/product")
                        .file(productPart(validProductRequest())))
                .andExpect(status().isBadRequest());
    }

    private MockMultipartFile productPart(ProductRequest request) throws Exception {
        return new MockMultipartFile(
                "product",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request)
        );
    }

    private MockMultipartFile imagePart() {
        return new MockMultipartFile(
                "imageFile",
                "phone.png",
                "image/png",
                "fake-image-content".getBytes()
        );
    }

    private ProductRequest validProductRequest() {
        return new ProductRequest(
                "Smart Phone",
                "Samsung",
                "A reliable Android smartphone",
                BigDecimal.valueOf(24999),
                "Mobiles",
                true,
                10
        );
    }

    private Product product() {
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
