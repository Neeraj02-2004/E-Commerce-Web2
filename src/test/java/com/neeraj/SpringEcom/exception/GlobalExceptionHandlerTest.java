package com.neeraj.SpringEcom.exception;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class GlobalExceptionHandlerTest {

    @Test
    void handleClientAbort_shouldNotThrowOrAttemptErrorResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");

        assertDoesNotThrow(() ->
                handler.handleClientAbort(
                        new AsyncRequestNotUsableException("Client disconnected"),
                        request
                )
        );
    }
}
