package com.serhat.ecommerce.commons.error;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * The single error body shape returned by every service, so clients get a consistent
 * contract instead of each service inventing its own.
 */
public record ApiError(
        String timestamp,
        int status,
        String error,
        String message,
        String path
) {

    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(Instant.now().toString(), status.value(), status.getReasonPhrase(), message, path);
    }
}
