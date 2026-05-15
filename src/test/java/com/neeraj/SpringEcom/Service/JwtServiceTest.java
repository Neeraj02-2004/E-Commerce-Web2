package com.neeraj.SpringEcom.Service;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            Base64.getEncoder().encodeToString("test-secret-key-test-secret-key-123456".getBytes());

    private static final String WRONG_SECRET =
            Base64.getEncoder().encodeToString("wrong-secret-key-wrong-secret-key-123".getBytes());

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-13T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void generateTokenAndValidateToken_roundTrip_shouldPass() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000, FIXED_CLOCK);

        UserDetails userDetails = user("user@example.com");
        String token = jwtService.generateToken(" USER@EXAMPLE.COM ");

        assertThat(jwtService.extractUserName(token)).isEqualTo("user@example.com");
        assertThat(jwtService.validateToken(token, userDetails)).isTrue();
    }

    @Test
    void validateToken_expiredToken_shouldReturnFalse() {
        JwtService issuingService = new JwtService(SECRET, 1_000, FIXED_CLOCK);
        String token = issuingService.generateToken("user@example.com");

        Clock laterClock = Clock.fixed(
                Instant.parse("2026-05-13T00:00:02Z"),
                ZoneOffset.UTC
        );

        JwtService validatingService = new JwtService(SECRET, 1_000, laterClock);

        assertThat(validatingService.validateToken(token, user("user@example.com"))).isFalse();
    }

    @Test
    void validateToken_tamperedToken_shouldReturnFalse() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000, FIXED_CLOCK);

        String token = jwtService.generateToken("user@example.com");
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.validateToken(tamperedToken, user("user@example.com"))).isFalse();
    }

    @Test
    void extractUserName_tamperedToken_shouldThrow() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000, FIXED_CLOCK);

        String token = jwtService.generateToken("user@example.com");
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.extractUserName(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateToken_wrongKey_shouldReturnFalse() {
        JwtService issuingService = new JwtService(SECRET, 3_600_000, FIXED_CLOCK);
        JwtService validatingService = new JwtService(WRONG_SECRET, 3_600_000, FIXED_CLOCK);

        String token = issuingService.generateToken("user@example.com");

        assertThat(validatingService.validateToken(token, user("user@example.com"))).isFalse();
    }

    @Test
    void validateToken_wrongUser_shouldReturnFalse() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000, FIXED_CLOCK);

        String token = jwtService.generateToken("user@example.com");

        assertThat(jwtService.validateToken(token, user("other@example.com"))).isFalse();
    }

    private UserDetails user(String username) {
        return User.withUsername(username)
                .password("password")
                .roles("USER")
                .build();
    }
}
