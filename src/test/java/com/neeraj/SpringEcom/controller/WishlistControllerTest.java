package com.neeraj.SpringEcom.controller;

import com.neeraj.SpringEcom.exception.GlobalExceptionHandler;
import com.neeraj.SpringEcom.model.dto.WishlistResponse;
import com.neeraj.SpringEcom.service.JwtService;
import com.neeraj.SpringEcom.service.MyUserDetailsService;
import com.neeraj.SpringEcom.service.WishlistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WishlistController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WishlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WishlistService wishlistService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private MyUserDetailsService myUserDetailsService;

    @Test
    void getWishlist_shouldReturnWishlistItems() throws Exception {
        when(wishlistService.getWishlist()).thenReturn(List.of(wishlistResponse()));

        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Phone"))
                .andExpect(jsonPath("$[0].brand").value("Test Brand"));

        verify(wishlistService).getWishlist();
    }

    @Test
    void addToWishlist_shouldReturnCreatedWishlistItem() throws Exception {
        when(wishlistService.addToWishlist(1L)).thenReturn(wishlistResponse());

        mockMvc.perform(post("/api/wishlist/1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Phone"));

        verify(wishlistService).addToWishlist(1L);
    }

    @Test
    void removeFromWishlist_shouldReturnRemovedMessage() throws Exception {
        mockMvc.perform(delete("/api/wishlist/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Removed from wishlist"));

        verify(wishlistService).removeFromWishlist(1L);
    }

    @Test
    void addToWishlist_withInvalidProductId_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/wishlist/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private WishlistResponse wishlistResponse() {
        return new WishlistResponse(
                1L,
                "Test Phone",
                "Test Brand",
                "A reliable test phone",
                new BigDecimal("25000.00"),
                "Mobiles",
                "/api/product-images/test-phone.png",
                true,
                5
        );
    }
}