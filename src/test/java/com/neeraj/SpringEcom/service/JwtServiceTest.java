package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.repo.UserRepo;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwtServiceTest {

    private static final String SECRET =
            Base64.getEncoder().encodeToString("test-secret-key-test-secret-key-123456".getBytes());

    private static final String WRONG_SECRET =
            Base64.getEncoder().encodeToString("wrong-secret-key-wrong-secret-key-123".getBytes());

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-13T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void generateTokenAndValidateToken_roundTrip_shouldPass() {
        UserRepo userRepo = mock(UserRepo.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(appUser("user@example.com", 0)));
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        JwtService jwtService = new JwtService(
                SECRET,
                3_600_000,
                FIXED_CLOCK,
                userRepo,
                redisTemplate
        );

        UserDetails userDetails = user("user@example.com");
        String token = jwtService.generateToken(" USER@EXAMPLE.COM ");

        assertThat(jwtService.extractUserName(token)).isEqualTo("user@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("USER");
        assertThat(jwtService.extractJti(token)).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.validateToken(token, userDetails)).isTrue();
    }

    @Test
    void generateToken_forAdminUser_shouldIncludeAdminRoleClaim() {
        UserRepo userRepo = mock(UserRepo.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        when(userRepo.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(appUser("admin@example.com", "ADMIN", 0)));
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        JwtService jwtService = new JwtService(
                SECRET,
                3_600_000,
                FIXED_CLOCK,
                userRepo,
                redisTemplate
        );

        String token = jwtService.generateToken("admin@example.com");

        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_expiredToken_shouldReturnFalse() {
        UserRepo userRepo = mock(UserRepo.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(appUser("user@example.com", 0)));

        JwtService issuingService = new JwtService(
                SECRET,
                1_000,
                FIXED_CLOCK,
                userRepo,
                redisTemplate
        );

        String token = issuingService.generateToken("user@example.com");

        Clock laterClock = Clock.fixed(
                Instant.parse("2026-05-13T00:00:02Z"),
                ZoneOffset.UTC
        );

        JwtService validatingService = new JwtService(
                SECRET,
                1_000,
                laterClock,
                userRepo,
                redisTemplate
        );

        assertThat(validatingService.validateToken(token)).isFalse();
        assertThat(validatingService.validateToken(token, user("user@example.com"))).isFalse();
    }

    @Test
    void validateToken_tamperedToken_shouldReturnFalse() {
        UserRepo userRepo = mock(UserRepo.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(appUser("user@example.com", 0)));

        JwtService jwtService = new JwtService(
                SECRET,
                3_600_000,
                FIXED_CLOCK,
                userRepo,
                redisTemplate
        );

        String token = jwtService.generateToken("user@example.com");
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.validateToken(tamperedToken)).isFalse();
        assertThat(jwtService.validateToken(tamperedToken, user("user@example.com"))).isFalse();
    }

    @Test
    void extractUserName_tamperedToken_shouldThrow() {
        UserRepo userRepo = mock(UserRepo.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(appUser("user@example.com", 0)));

        JwtService jwtService = new JwtService(
                SECRET,
                3_600_000,
                FIXED_CLOCK,
                userRepo,
                redisTemplate
        );

        String token = jwtService.generateToken("user@example.com");
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.extractUserName(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateToken_wrongKey_shouldReturnFalse() {
        UserRepo userRepo = mock(UserRepo.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(appUser("user@example.com", 0)));

        JwtService issuingService = new JwtService(
                SECRET,
                3_600_000,
                FIXED_CLOCK,
                userRepo,
                redisTemplate
        );

        JwtService validatingService = new JwtService(
                WRONG_SECRET,
                3_600_000,
                FIXED_CLOCK,
                userRepo,
                redisTemplate
        );

        String token = issuingService.generateToken("user@example.com");

        assertThat(validatingService.validateToken(token)).isFalse();
        assertThat(validatingService.validateToken(token, user("user@example.com"))).isFalse();
    }

    @Test
    void validateToken_wrongUser_shouldReturnFalse() {
        UserRepo userRepo = mock(UserRepo.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(appUser("user@example.com", 0)));
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        JwtService jwtService = new JwtService(
                SECRET,
                3_600_000,
                FIXED_CLOCK,
                userRepo,
                redisTemplate
        );

        String token = jwtService.generateToken("user@example.com");

        assertThat(jwtService.validateToken(token, user("other@example.com"))).isFalse();
    }

    @Test
    void validateToken_revokedToken_shouldReturnFalse() {
        UserRepo userRepo = mock(UserRepo.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(appUser("user@example.com", 0)));
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        JwtService jwtService = new JwtService(
                SECRET,
                3_600_000,
                FIXED_CLOCK,
                userRepo,
                redisTemplate
        );

        String token = jwtService.generateToken("user@example.com");

        assertThat(jwtService.validateToken(token)).isFalse();
        assertThat(jwtService.validateToken(token, user("user@example.com"))).isFalse();
    }

    @Test
    void validateToken_tokenVersionMismatch_shouldReturnFalse() {
        UserRepo userRepo = mock(UserRepo.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(userRepo.findByEmail("user@example.com"))
                .thenReturn(Optional.of(appUser("user@example.com", 0)))
                .thenReturn(Optional.of(appUser("user@example.com", 1)));

        JwtService jwtService = new JwtService(
                SECRET,
                3_600_000,
                FIXED_CLOCK,
                userRepo,
                redisTemplate
        );

        String token = jwtService.generateToken("user@example.com");

        assertThat(jwtService.validateToken(token)).isFalse();
        assertThat(jwtService.validateToken(token, user("user@example.com"))).isFalse();
    }

    @Test
    void revokeToken_shouldStoreJtiInRedisWithRemainingTtl() {
        UserRepo userRepo = mock(UserRepo.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(appUser("user@example.com", 0)));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        JwtService jwtService = new JwtService(
                SECRET,
                3_600_000,
                FIXED_CLOCK,
                userRepo,
                redisTemplate
        );

        String token = jwtService.generateToken("user@example.com");

        jwtService.revokeToken(token);

        verify(valueOperations).set(
                startsWith("jwt:revoked:"),
                eq("true"),
                any()
        );
    }

    @Test
    void revokeAllTokens_shouldIncrementUserTokenVersion() {
        UserRepo userRepo = mock(UserRepo.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        com.neeraj.SpringEcom.model.User appUser = appUser("user@example.com", 2);

        when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(appUser));

        JwtService jwtService = new JwtService(
                SECRET,
                3_600_000,
                FIXED_CLOCK,
                userRepo,
                redisTemplate
        );

        jwtService.revokeAllTokens("user@example.com");

        assertThat(appUser.getTokenVersion()).isEqualTo(3);
        verify(userRepo).save(appUser);
    }

    private UserDetails user(String username) {
        return User.withUsername(username)
                .password("password")
                .roles("USER")
                .build();
    }

    private com.neeraj.SpringEcom.model.User appUser(String email, int tokenVersion) {
        return appUser(email, "USER", tokenVersion);
    }

    private com.neeraj.SpringEcom.model.User appUser(String email, String role, int tokenVersion) {
        com.neeraj.SpringEcom.model.User user = new com.neeraj.SpringEcom.model.User();
        user.setId(1);
        user.setUsername("Neeraj");
        user.setEmail(email);
        user.setPassword("password");
        user.setProvider("LOCAL");
        user.setRole(role);
        user.setTokenVersion(tokenVersion);
        return user;
    }
}