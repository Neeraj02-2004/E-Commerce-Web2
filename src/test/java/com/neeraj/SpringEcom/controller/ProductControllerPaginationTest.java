package com.neeraj.SpringEcom.controller;

import com.neeraj.SpringEcom.exception.GlobalExceptionHandler;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.service.JwtService;
import com.neeraj.SpringEcom.service.MyUserDetailsService;
import com.neeraj.SpringEcom.service.ProductService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ProductControllerPaginationTest {

    @Resource
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private MyUserDetailsService myUserDetailsService;

    @Test
    void getProducts_withDefaultPagination_shouldReturnPagedProducts() throws Exception {
        Product product = product(10L);

        when(productService.getProducts(0, 20))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].name").value("Samsung 55 Inch"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(productService).getProducts(0, 20);
    }

    @Test
    void getProducts_withCustomPagination_shouldUseRequestedPageAndSize() throws Exception {
        when(productService.getProducts(1, 5))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 13));

        mockMvc.perform(get("/api/products")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.totalElements").value(13));

        verify(productService).getProducts(1, 5);
    }

    @Test
    void getProducts_withInvalidSize_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setName("Samsung 55 Inch");
        product.setBrand("Samsung");
        product.setDescription("Large 4K smart TV");
        product.setPrice(BigDecimal.valueOf(47000));
        product.setCategory("Electronics");
        product.setReleaseDate(LocalDate.of(2026, 5, 24));
        product.setProductAvailable(true);
        product.setStockQuantity(8);
        product.setImageName("tv.png");
        product.setImageType("image/png");
        product.setImageUrl("/api/product-images/tv.png");
        return product;
    }
}