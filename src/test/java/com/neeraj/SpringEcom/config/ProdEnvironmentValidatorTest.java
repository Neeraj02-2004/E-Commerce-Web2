package com.neeraj.SpringEcom.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProdEnvironmentValidatorTest {

    @Test
    void run_whenJwtSecretIsNotBase64_shouldRejectProductionConfig() {
        ProdEnvironmentValidator validator = new ProdEnvironmentValidator(
                environmentWithJwtSecret("this-is-long-enough-but-not-base64-secret")
        );

        assertThatThrownBy(() -> validator.run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must be valid Base64");
    }

    @Test
    void run_whenJwtSecretDecodesToLessThan32Bytes_shouldRejectProductionConfig() {
        String shortSecret = Base64.getEncoder().encodeToString("123456789012345678901234".getBytes());
        ProdEnvironmentValidator validator = new ProdEnvironmentValidator(environmentWithJwtSecret(shortSecret));

        assertThatThrownBy(() -> validator.run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must decode to at least 32 bytes");
    }

    @Test
    void run_whenJwtSecretIsStrongBase64_shouldAcceptProductionConfig() {
        String validSecret = Base64.getEncoder()
                .encodeToString("strong-secret-with-at-least-thirty-two-bytes".getBytes());
        ProdEnvironmentValidator validator = new ProdEnvironmentValidator(environmentWithJwtSecret(validSecret));

        assertThatCode(() -> validator.run(mock(ApplicationArguments.class)))
                .doesNotThrowAnyException();
    }

    private Environment environmentWithJwtSecret(String jwtSecret) {
        Environment environment = mock(Environment.class);

        when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:postgresql://db:5432/springecom");
        when(environment.getProperty("spring.datasource.username")).thenReturn("springecom");
        when(environment.getProperty("spring.datasource.password")).thenReturn("strong-db-password");
        when(environment.getProperty("spring.data.redis.host")).thenReturn("redis");
        when(environment.getProperty("spring.data.redis.password")).thenReturn("strong-redis-password");
        when(environment.getProperty("spring.security.oauth2.client.registration.google.client-id"))
                .thenReturn("google-client-id");
        when(environment.getProperty("spring.security.oauth2.client.registration.google.client-secret"))
                .thenReturn("google-client-secret");
        when(environment.getProperty("razorpay.key-id")).thenReturn("razorpay-key-id");
        when(environment.getProperty("razorpay.key-secret")).thenReturn("razorpay-key-secret");
        when(environment.getProperty("razorpay.webhook-secret")).thenReturn("razorpay-webhook-secret");
        when(environment.getProperty("app.cors.origins")).thenReturn("https://shop.example.com");
        when(environment.getProperty("app.cors.origins", "")).thenReturn("https://shop.example.com");
        when(environment.getProperty("app.jwt.secret")).thenReturn(jwtSecret);
        when(environment.getProperty("app.storage.type", "cloudinary")).thenReturn("cloudinary");
        when(environment.getProperty("cloudinary.cloud-name")).thenReturn("cloud-name");
        when(environment.getProperty("cloudinary.api-key")).thenReturn("cloudinary-api-key");
        when(environment.getProperty("cloudinary.api-secret")).thenReturn("cloudinary-api-secret");

        return environment;
    }
}
