package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.exception.AuthException;
import com.neeraj.SpringEcom.model.User;
import com.neeraj.SpringEcom.repo.UserRepo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.neeraj.SpringEcom.util.EmailNormalizer;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String TOKEN_VERSION_CLAIM = "tokenVersion";
    private static final String ROLE_CLAIM = "role";
    private static final String REVOKED_TOKEN_KEY_PREFIX = "jwt:revoked:";

    private final String secretKey;
    private final long jwtExpirationMs;
    private final Clock clock;
    private final UserRepo userRepo;
    private final StringRedisTemplate stringRedisTemplate;
    private final EmailNormalizer emailNormalizer;

    @Autowired
    public JwtService(
            @Value("${app.jwt.secret}") String secretKey,
            @Value("${app.jwt.expiration-ms:3600000}") long jwtExpirationMs,
            UserRepo userRepo,
            StringRedisTemplate stringRedisTemplate,
            EmailNormalizer emailNormalizer
    ) {
        this(secretKey, jwtExpirationMs, Clock.systemUTC(), userRepo, stringRedisTemplate, emailNormalizer);
    }

    public JwtService(
            String secretKey,
            long jwtExpirationMs,
            Clock clock,
            UserRepo userRepo,
            StringRedisTemplate stringRedisTemplate
    ) {
        this(secretKey, jwtExpirationMs, clock, userRepo, stringRedisTemplate, new EmailNormalizer());
    }

    public JwtService(
            String secretKey,
            long jwtExpirationMs,
            Clock clock,
            UserRepo userRepo,
            StringRedisTemplate stringRedisTemplate,
            EmailNormalizer emailNormalizer
    ) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("JWT secret is required");
        }

        if (jwtExpirationMs <= 0) {
            throw new IllegalArgumentException("JWT expiration must be greater than zero");
        }

        this.secretKey = secretKey;
        this.jwtExpirationMs = jwtExpirationMs;
        this.clock = clock;
        this.userRepo = userRepo;
        this.stringRedisTemplate = stringRedisTemplate;
        this.emailNormalizer = emailNormalizer;
    }

    public String generateToken(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required to generate JWT token");
        }

        String identity = emailNormalizer.normalize(email);
        User user = userRepo.findByEmail(identity)
                .orElseThrow(() -> new AuthException("User not found"));

        if (user.getRole() == null || user.getRole().isBlank()) {
            throw new AuthException("User role is missing");
        }

        long now = clock.millis();

        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_VERSION_CLAIM, user.getTokenVersion() == null ? 0 : user.getTokenVersion());
        claims.put(ROLE_CLAIM, user.getRole().trim().toUpperCase());

        return Jwts.builder()
                .claims(claims)
                .id(UUID.randomUUID().toString())
                .subject(identity)
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtExpirationMs))
                .signWith(getKey())
                .compact();
    }

    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get(ROLE_CLAIM, String.class));
    }

    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            String tokenUser = extractUserName(token);
            String role = extractRole(token);

            if (tokenUser == null || tokenUser.isBlank()) {
                return false;
            }

            if (role == null || role.isBlank()) {
                return false;
            }

            return !isTokenExpired(token)
                    && !isTokenRevoked(token)
                    && tokenVersionMatches(token, tokenUser);

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        if (token == null || token.isBlank() || userDetails == null) {
            return false;
        }

        try {
            String tokenUser = extractUserName(token);
            String systemUser = userDetails.getUsername();

            if (tokenUser == null || systemUser == null) {
                return false;
            }

            return emailNormalizer.equalsNormalized(tokenUser, systemUser)
                    && validateToken(token)
                    && userDetails.isEnabled()
                    && userDetails.isAccountNonLocked()
                    && userDetails.isAccountNonExpired()
                    && userDetails.isCredentialsNonExpired();

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        try {
            String jti = extractJti(token);

            if (jti == null || jti.isBlank()) {
                return;
            }

            long remainingMs = extractExpiration(token).getTime() - clock.millis();

            if (remainingMs <= 0) {
                return;
            }

            stringRedisTemplate.opsForValue().set(
                    revokedTokenKey(jti),
                    "true",
                    Duration.ofMillis(remainingMs)
            );
        } catch (JwtException | IllegalArgumentException ignored) {
        }
    }

    @Transactional
    public void revokeAllTokens(String email) {
        if (email == null || email.isBlank()) {
            throw new AuthException("User not found");
        }

        String normalizedEmail = emailNormalizer.normalize(email);

        User user = userRepo.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthException("User not found"));

        int currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        user.setTokenVersion(currentVersion + 1);
        userRepo.save(user);
    }

    private boolean tokenVersionMatches(String token, String email) {
        Integer tokenVersion = extractTokenVersion(token);

        if (tokenVersion == null) {
            return false;
        }

        String normalizedEmail = emailNormalizer.normalize(email);

        return userRepo.findByEmail(normalizedEmail)
                .map(user -> {
                    int currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
                    return currentVersion == tokenVersion;
                })
                .orElse(false);
    }

    private Integer extractTokenVersion(String token) {
        Object value = extractClaim(token, claims -> claims.get(TOKEN_VERSION_CLAIM));

        if (value instanceof Integer integerValue) {
            return integerValue;
        }

        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }

        return null;
    }

    private boolean isTokenRevoked(String token) {
        String jti = extractJti(token);

        if (jti == null || jti.isBlank()) {
            return true;
        }

        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(revokedTokenKey(jti)));
    }

    private String revokedTokenKey(String jti) {
        return REVOKED_TOKEN_KEY_PREFIX + jti;
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .clock(() -> new Date(clock.millis()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date(clock.millis()));
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

}