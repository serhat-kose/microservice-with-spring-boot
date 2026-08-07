package com.serhat.ecommerce.productservice.exception;

import com.serhat.ecommerce.commons.error.ApiError;
import com.serhat.ecommerce.commons.error.BaseExceptionHandler;
import com.serhat.ecommerce.productservice.exception.CatalogExceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler extends BaseExceptionHandler {

    @ExceptionHandler({ProductNotFoundException.class, CategoryNotFoundException.class,
            BrandNotFoundException.class, SellerNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateSkuException.class)
    public ResponseEntity<ApiError> handleDuplicateSku(DuplicateSkuException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(CategoryCycleException.class)
    public ResponseEntity<ApiError> handleCycle(CategoryCycleException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Raised by the {@code @Version} check when two edits to the same product collide;
     * 409 tells the caller to re-read and retry rather than suggesting a server fault.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleConcurrentEdit(OptimisticLockingFailureException ex,
                                                         HttpServletRequest request) {
        return build(HttpStatus.CONFLICT,
                "This product was modified concurrently, please reload and try again", request);
    }
}
