package com.neeraj.SpringEcom.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String RATE_LIMIT_KEY_PREFIX = "rate-limit:";

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final Set<String> trustedProxies;

    public RateLimitFilter(
            ObjectMapper objectMapper,
            @Nullable StringRedisTemplate stringRedisTemplate,
            @Value("${app.rate-limit.trusted-proxies:}") String trustedProxies
    ) {
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.trustedProxies = parseTrustedProxies(trustedProxies);
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

        String key = RATE_LIMIT_KEY_PREFIX + rule.keyPrefix() + ":" + resolveClientKey(request, rule);

        if (isAllowed(key, rule)) {
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

    private boolean isAllowed(String key, RateLimitRule rule) {
        if (stringRedisTemplate == null) {
            return true;
        }

        try {
            Long requestCount = stringRedisTemplate.opsForValue().increment(key);

            if (requestCount != null && requestCount == 1L) {
                stringRedisTemplate.expire(key, rule.window());
            }

            return requestCount == null || requestCount <= rule.capacity();
        } catch (RuntimeException ex) {
            log.warn("Rate limit check failed. Allowing request. key={}", key, ex);
            return true;
        }
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
        if (isTrustedProxy(request.getRemoteAddr())) {
            String forwardedForIp = firstForwardedForIp(request.getHeader("X-Forwarded-For"));

            if (forwardedForIp != null) {
                return forwardedForIp;
            }

            String realIp = request.getHeader("X-Real-IP");

            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }

        return request.getRemoteAddr();
    }

    private String firstForwardedForIp(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }

        String firstIp = forwardedFor.split(",")[0].trim();

        if (firstIp.isBlank()) {
            return null;
        }

        return firstIp;
    }

    private Set<String> parseTrustedProxies(String trustedProxies) {
        if (trustedProxies == null || trustedProxies.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(proxy -> !proxy.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean isTrustedProxy(String remoteAddr) {
        return remoteAddr != null && trustedProxies.contains(remoteAddr.trim());
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