package com.neeraj.SpringEcom;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neeraj.SpringEcom.Service.JwtService;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.model.User;
import com.neeraj.SpringEcom.model.dto.OrderItemRequest;
import com.neeraj.SpringEcom.model.dto.OrderRequest;
import com.neeraj.SpringEcom.repo.OrderRepo;
import com.neeraj.SpringEcom.repo.ProductRepo;
import com.neeraj.SpringEcom.repo.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class OrderPlacementIntegrationTest {

    private static final String USER_EMAIL = "buyer@example.com";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("springecom_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.open-in-view", () -> "false");

        registry.add("spring.cache.type", () -> "redis");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        registry.add("app.jwt.secret", () -> "bFJiTGtSdXFXcXJyQkRCZVNnRys4MXlsNEdINkFmQVdYTWFiWWxtbCtNPQ==");
        registry.add("app.jwt.expiration-ms", () -> "3600000");
        registry.add("app.cors.origins", () -> "http://localhost:5173");

        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> "test-google-client-id");
        registry.add("spring.security.oauth2.client.registration.google.client-secret", () -> "test-google-client-secret");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        orderRepo.deleteAll();
        productRepo.deleteAll();
        userRepo.deleteAll();

        stringRedisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();

        User user = new User();
        user.setUsername("Buyer");
        user.setEmail(USER_EMAIL);
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setProvider("LOCAL");
        user.setRole("USER");
        userRepo.save(user);
    }

    @Test
    void placeOrder_withRedisAndPostgres_shouldCreateOrderAndDecrementStock() throws Exception {
        Product product = new Product();
        product.setName("Test Phone");
        product.setBrand("Test Brand");
        product.setDescription("Integration test product");
        product.setCategory("Mobiles");
        product.setPrice(new BigDecimal("25000.00"));
        product.setProductAvailable(true);
        product.setStockQuantity(5);

        Product savedProduct = productRepo.save(product);

        OrderRequest request = new OrderRequest(
                "Neeraj Kumar",
                USER_EMAIL,
                "9876543210",
                List.of(new OrderItemRequest(savedProduct.getId(), 2))
        );

        String token = jwtService.generateToken(USER_EMAIL);

        mockMvc.perform(post("/api/place")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.customerName").value("Neeraj Kumar"))
                .andExpect(jsonPath("$.email").value(USER_EMAIL))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.items[0].productName").value("Test Phone"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        Product updatedProduct = productRepo.findById(savedProduct.getId()).orElseThrow();

        assertThat(updatedProduct.getStockQuantity()).isEqualTo(3);
        assertThat(orderRepo.findAll()).hasSize(1);
        assertThat(cacheManager.getCacheNames()).isNotEmpty();
    }
}
