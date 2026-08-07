package com.serhat.ecommerce.cartservice.exception;

import com.serhat.ecommerce.commons.error.ApiError;
import com.serhat.ecommerce.commons.error.BaseExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler extends BaseExceptionHandler {

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(CartNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }
}
