package com.neeraj.SpringEcom.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    @Test
    void login_shouldAllowFirstTenRequestsAndRejectEleventhFromSameIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new ObjectMapper(), redisTemplate(), "");
        FilterChain filterChain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = request("POST", "/api/login", "10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isNotEqualTo(HTTP_TOO_MANY_REQUESTS);
        }

        MockHttpServletRequest eleventhRequest = request("POST", "/api/login", "10.0.0.1");
        MockHttpServletResponse eleventhResponse = new MockHttpServletResponse();

        filter.doFilter(eleventhRequest, eleventhResponse, filterChain);

        assertThat(eleventhResponse.getStatus()).isEqualTo(HTTP_TOO_MANY_REQUESTS);
        assertThat(eleventhResponse.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void login_shouldUseForwardedForOnlyWhenRemoteAddressIsTrustedProxy() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new ObjectMapper(), redisTemplate(), "10.0.0.1");
        FilterChain filterChain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = request("POST", "/api/login", "10.0.0.1");
            request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isNotEqualTo(HTTP_TOO_MANY_REQUESTS);
        }

        MockHttpServletRequest eleventhRequest = request("POST", "/api/login", "10.0.0.1");
        eleventhRequest.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        MockHttpServletResponse eleventhResponse = new MockHttpServletResponse();

        filter.doFilter(eleventhRequest, eleventhResponse, filterChain);

        assertThat(eleventhResponse.getStatus()).isEqualTo(HTTP_TOO_MANY_REQUESTS);
    }

    @Test
    void login_shouldIgnoreForwardedForWhenRemoteAddressIsNotTrustedProxy() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new ObjectMapper(), redisTemplate(), "192.168.1.10");
        FilterChain filterChain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = request("POST", "/api/login", "10.0.0.1");
            request.addHeader("X-Forwarded-For", "203.0.113." + i);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isNotEqualTo(HTTP_TOO_MANY_REQUESTS);
        }

        MockHttpServletRequest eleventhRequest = request("POST", "/api/login", "10.0.0.1");
        eleventhRequest.addHeader("X-Forwarded-For", "203.0.113.99");
        MockHttpServletResponse eleventhResponse = new MockHttpServletResponse();

        filter.doFilter(eleventhRequest, eleventhResponse, filterChain);

        assertThat(eleventhResponse.getStatus()).isEqualTo(HTTP_TOO_MANY_REQUESTS);
    }

    @Test
    void unrelatedEndpoint_shouldNotBeRateLimited() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new ObjectMapper(), redisTemplate(), "");
        FilterChain filterChain = mock(FilterChain.class);

        for (int i = 0; i < 50; i++) {
            MockHttpServletRequest request = request("GET", "/api/orders", "10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isNotEqualTo(HTTP_TOO_MANY_REQUESTS);
        }
    }

    private StringRedisTemplate redisTemplate() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        Map<String, Long> counters = new ConcurrentHashMap<>();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
        when(valueOperations.increment(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return counters.merge(key, 1L, Long::sum);
        });

        return redisTemplate;
    }

    private MockHttpServletRequest request(String method, String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}