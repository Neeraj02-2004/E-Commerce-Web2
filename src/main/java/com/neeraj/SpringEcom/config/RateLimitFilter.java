package com.neeraj.SpringEcom.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        RateLimitRule rule = resolveRule(request);

        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = rule.keyPrefix() + ":" + resolveClientKey(request, rule);
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> createBucket(rule));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        writeTooManyRequests(response);
    }

    private RateLimitRule resolveRule(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (HttpMethod.OPTIONS.matches(method)) {
            return null;
        }

        if (HttpMethod.POST.matches(method) && "/api/login".equals(path)) {
            return RateLimitRule.ip("login", 10, Duration.ofMinutes(5));
        }

        if (HttpMethod.POST.matches(method) && "/api/login/google".equals(path)) {
            return RateLimitRule.ip("google-login", 10, Duration.ofMinutes(5));
        }

        if (HttpMethod.POST.matches(method) && "/api/register".equals(path)) {
            return RateLimitRule.ip("register", 10, Duration.ofMinutes(5));
        }

        if (HttpMethod.POST.matches(method) && "/api/payments/create".equals(path)) {
            return RateLimitRule.authenticatedUser("payment-create", 60, Duration.ofHours(1));
        }

        if (HttpMethod.GET.matches(method)
                && ("/api/products".equals(path) || "/api/products/search".equals(path))) {
            return RateLimitRule.ip("products-read", 300, Duration.ofMinutes(5));
        }

        return null;
    }

    private String resolveClientKey(HttpServletRequest request, RateLimitRule rule) {
        if (rule.keyType() == RateLimitKeyType.AUTHENTICATED_USER) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication.getName() != null
                    && !"anonymousUser".equals(authentication.getName())) {
                return authentication.getName().toLowerCase().trim();
            }
        }

        return clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");

        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private Bucket createBucket(RateLimitRule rule) {
        Bandwidth limit = Bandwidth.classic(
                rule.capacity(),
                Refill.intervally(rule.capacity(), rule.window())
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(
                response.getWriter(),
                Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 429,
                        "error", "RATE_LIMIT_EXCEEDED",
                        "message", "Too many requests. Please try again later."
                )
        );
    }

    private record RateLimitRule(
            String keyPrefix,
            RateLimitKeyType keyType,
            long capacity,
            Duration window
    ) {
        static RateLimitRule ip(String keyPrefix, long capacity, Duration window) {
            return new RateLimitRule(keyPrefix, RateLimitKeyType.IP, capacity, window);
        }

        static RateLimitRule authenticatedUser(String keyPrefix, long capacity, Duration window) {
            return new RateLimitRule(keyPrefix, RateLimitKeyType.AUTHENTICATED_USER, capacity, window);
        }
    }

    private enum RateLimitKeyType {
        IP,
        AUTHENTICATED_USER
    }
}