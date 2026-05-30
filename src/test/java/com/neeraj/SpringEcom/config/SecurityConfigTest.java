package com.neeraj.SpringEcom.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.neeraj.SpringEcom.controller.OrderController;
import com.neeraj.SpringEcom.controller.ProductController;
import com.neeraj.SpringEcom.controller.UserController;
import com.neeraj.SpringEcom.repo.UserRepo;
import com.neeraj.SpringEcom.service.JwtService;
import com.neeraj.SpringEcom.service.MyUserDetailsService;
import com.neeraj.SpringEcom.service.OrderService;
import com.neeraj.SpringEcom.service.ProductService;
import com.neeraj.SpringEcom.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        ProductController.class,
        OrderController.class,
        UserController.class
})
@Import({
        SecurityConfig.class,
        JwtFilter.class,
        RateLimitFilter.class
})
@TestPropertySource(properties = {
        "app.cors.origins=http://localhost:5173"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private MyUserDetailsService myUserDetailsService;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepo userRepo;

    @MockBean
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @MockBean
    private CacheManager cacheManager;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUpRateLimitRedis() {
        valueOperations = mock(ValueOperations.class);
        Map<String, Long> counters = new ConcurrentHashMap<>();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
        when(valueOperations.increment(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return counters.merge(key, 1L, Long::sum);
        });
    }

    @Test
    void optionsRequest_shouldBePublic() throws Exception {
        mockMvc.perform(options("/api/place"))
                .andExpect(status().isOk());
    }

    @Test
    void publicProductEndpoints_shouldAllowAnonymous() throws Exception {
        when(productService.getProducts(anyInt(), anyInt()))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));
        when(productService.searchProducts("phone")).thenReturn(List.of());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products/search").param("keyword", "phone"))
                .andExpect(status().isOk());
    }

    @Test
    void orderEndpoints_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/place")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Neeraj Kumar",
                                  "email": "buyer@example.com",
                                  "mobileNo": "9876543210",
                                  "items": [
                                    {
                                      "productId": 1,
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/cancel/ORD123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void orderEndpoints_shouldAllowUserRole() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .with(user("buyer@example.com").roles("USER")))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/cancel/ORD123")
                        .with(user("buyer@example.com").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void orderEndpoints_shouldRejectAdminRole() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/place")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Admin User",
                                  "email": "admin@example.com",
                                  "mobileNo": "9876543210",
                                  "items": [
                                    {
                                      "productId": 1,
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/cancel/ORD123")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void wishlistEndpoints_shouldRequireAuthenticationAndRejectAdminRole() throws Exception {
        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/wishlist")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void paymentEndpoints_shouldRequireAuthenticationAndRejectAdminRole() throws Exception {
        mockMvc.perform(post("/api/payments/create"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/payments/create")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rateLimitFilter_shouldApplyInsideSecurityChain() throws Exception {
        String clientIp = "203.0.113.77";

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/login")
                            .header("X-Forwarded-For", clientIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(result ->
                            assertThat(result.getResponse().getStatus()).isNotEqualTo(429)
                    );
        }

        mockMvc.perform(post("/api/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("RATE_LIMIT_EXCEEDED")));
    }

    @Test
    void userReturnExchangeEndpoints_shouldRejectAdminRole() throws Exception {
        mockMvc.perform(post("/api/orders/ORD123/return-exchange")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestType": "RETURN",
                                  "reason": "Product is defective and not working"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/orders/return-exchange")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminProductWriteEndpoints_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(delete("/api/admin/product/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/admin/product/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/product"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminProductWriteEndpoints_shouldRejectUserRole() throws Exception {
        mockMvc.perform(delete("/api/admin/product/1")
                        .with(user("buyer@example.com").roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/product/1")
                        .with(user("buyer@example.com").roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/product")
                        .with(user("buyer@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminProductWriteEndpoints_shouldAllowAdminRole() throws Exception {
        doNothing().when(productService).deleteProduct(anyLong());

        mockMvc.perform(delete("/api/admin/product/1")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void adminReturnExchangeEndpoints_shouldRejectUserRole() throws Exception {
        mockMvc.perform(get("/api/admin/return-exchange")
                        .with(user("buyer@example.com").roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/return-exchange/REX123/approve")
                        .with(user("buyer@example.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "adminNote": "Approved"
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}