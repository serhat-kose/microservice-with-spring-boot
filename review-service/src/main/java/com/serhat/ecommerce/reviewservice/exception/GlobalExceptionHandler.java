package com.serhat.ecommerce.reviewservice.exception;

import com.serhat.ecommerce.commons.error.ApiError;
import com.serhat.ecommerce.commons.error.BaseExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler extends BaseExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNotFound(NoSuchElementException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /**
     * The one-review-per-customer rule is enforced by a unique constraint, so two concurrent
     * submissions surface here rather than through the service's pre-check.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDuplicate(DataIntegrityViolationException ex,
                                                     HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "You have already reviewed this product", request);
    }
}
