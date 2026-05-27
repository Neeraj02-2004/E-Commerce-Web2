package com.neeraj.SpringEcom.exception;

import com.neeraj.SpringEcom.config.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiError> handleProductNotFound(
            ProductNotFoundException e,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", e.getMessage(), request);
    }

    @ExceptionHandler(InvalidProductDataException.class)
    public ResponseEntity<ApiError> handleInvalidProductData(
            InvalidProductDataException e,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_DATA", e.getMessage(), request);
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiError> handleFileStorage(
            FileStorageException e,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "FILE_STORAGE_ERROR", e.getMessage(), request);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiError> handleOrderNotFound(
            OrderNotFoundException e,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", e.getMessage(), request);
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ApiError> handleInvalidOrder(
            InvalidOrderException e,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_ORDER", e.getMessage(), request);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> handleInsufficientStock(
            InsufficientStockException e,
            HttpServletRequest request
    ) {
        return error(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", e.getMessage(), request);
    }

    @ExceptionHandler(OrderAlreadyCancelledException.class)
    public ResponseEntity<ApiError> handleOrderAlreadyCancelled(
            OrderAlreadyCancelledException e,
            HttpServletRequest request
    ) {
        return error(HttpStatus.CONFLICT, "ORDER_ALREADY_CANCELLED", e.getMessage(), request);
    }

    @ExceptionHandler(WishlistItemAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleWishlistItemAlreadyExists(
            WishlistItemAlreadyExistsException e,
            HttpServletRequest request
    ) {
        return error(HttpStatus.CONFLICT, "WISHLIST_ITEM_ALREADY_EXISTS", e.getMessage(), request);
    }

    @ExceptionHandler(InvalidUserException.class)
    public ResponseEntity<ApiError> handleInvalidUser(
            InvalidUserException e,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_USER", e.getMessage(), request);
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException e,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.ACCEPTED,
                "REGISTRATION_REQUEST_RECEIVED",
                "Registration request received. If this email can be registered, you may continue with login.",
                request
        );
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handleAuth(
            AuthException e,
            HttpServletRequest request
    ) {
        return error(HttpStatus.UNAUTHORIZED, "AUTH_ERROR", e.getMessage(), request);
    }

    @ExceptionHandler(UserNotAuthenticatedException.class)
    public ResponseEntity<ApiError> handleUserNotAuthenticated(
            UserNotAuthenticatedException e,
            HttpServletRequest request
    ) {
        return error(HttpStatus.UNAUTHORIZED, "USER_NOT_AUTHENTICATED", e.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(
            BadCredentialsException e,
            HttpServletRequest request
    ) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException e,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "You do not have permission to access this resource",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        String message = e.getConstraintViolations()
                .stream()
                .findFirst()
                .map(error -> error.getPropertyPath() + ": " + error.getMessage())
                .orElse("Validation failed");

        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<ApiError> handleEmptyResult(
            EmptyResultDataAccessException e,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Requested resource not found", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException e,
            HttpServletRequest request
    ) {
        String requestId = RequestIdFilter.getRequestId(request);
        log.warn("Bad request caused by illegal argument. requestId={}", requestId, e);

        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request", request);
    }


    @ExceptionHandler({
            AsyncRequestNotUsableException.class,
            ClientAbortException.class
    })
    public void handleClientAbort(
            Exception e,
            HttpServletRequest request
    ) {
        String requestId = RequestIdFilter.getRequestId(request);

        log.debug(
                "Client closed the connection before the response completed. requestId={}, path={}",
                requestId,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(
            ResponseStatusException e,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        String message = e.getReason() != null ? e.getReason() : status.getReasonPhrase();

        return error(status, "REQUEST_ERROR", message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception e,
            HttpServletRequest request
    ) {
        String requestId = RequestIdFilter.getRequestId(request);
        log.error("Unhandled exception. requestId={}", requestId, e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        Instant.now(),
                        request.getRequestURI(),
                        requestId,
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "INTERNAL_ERROR",
                        "Something went wrong"
                ));
    }

    private ResponseEntity<ApiError> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(status)
                .body(new ApiError(
                        Instant.now(),
                        request.getRequestURI(),
                        RequestIdFilter.getRequestId(request),
                        status.value(),
                        code,
                        message
                ));
    }
}