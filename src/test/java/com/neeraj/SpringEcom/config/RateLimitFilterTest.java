package com.neeraj.SpringEcom.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RateLimitFilterTest {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    @Test
    void login_shouldAllowFirstTenRequestsAndRejectEleventhFromSameIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new ObjectMapper());
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
    void login_shouldUseForwardedForAsClientIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new ObjectMapper());
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
    void unrelatedEndpoint_shouldNotBeRateLimited() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new ObjectMapper());
        FilterChain filterChain = mock(FilterChain.class);

        for (int i = 0; i < 50; i++) {
            MockHttpServletRequest request = request("GET", "/api/orders", "10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isNotEqualTo(HTTP_TOO_MANY_REQUESTS);
        }
    }

    private MockHttpServletRequest request(String method, String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}