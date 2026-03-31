package service.commons.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response record used across all microservices.
 * Eliminates duplication of error response DTOs.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(LocalDateTime.now(), status, error, message, path, null);
    }

    public ErrorResponse(int status, String error, String message, String path, Map<String, String> validationErrors) {
        this(LocalDateTime.now(), status, error, message, path, validationErrors);
    }
}

