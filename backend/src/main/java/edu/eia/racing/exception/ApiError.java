package edu.eia.racing.exception;

import java.time.LocalDateTime;

/**
 * Structured error contract required by PROJECT_SPEC section 5.
 * Stack traces and internal details must never leak through this payload.
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(LocalDateTime.now(), status, error, message, path);
    }
}
