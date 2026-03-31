package service.media.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for media service.
 * Handles MediaNotFoundException and other exceptions to return appropriate HTTP status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String STATUS_KEY = "status";
    private static final String MESSAGE_KEY = "message";
    private static final String ERROR_STATUS = "error";

    @ExceptionHandler(MediaNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleMediaNotFound(MediaNotFoundException ex) {
        log.warn("Media not found: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put(STATUS_KEY, ERROR_STATUS);
        response.put(MESSAGE_KEY, "Media not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put(STATUS_KEY, ERROR_STATUS);
        response.put(MESSAGE_KEY, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<Map<String, String>> handleInvalidFileException(InvalidFileException ex) {
        log.warn("Invalid file: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put(STATUS_KEY, ERROR_STATUS);
        response.put(MESSAGE_KEY, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}

