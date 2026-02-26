package service.order.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import service.order.exceptions.InsufficientFundsException;
import service.order.exceptions.InsufficientStockException;
import service.order.exceptions.OrderAccessDeniedException;
import service.order.exceptions.OrderNotFoundException;
import service.order.exceptions.OrderServiceException;
import service.order.exceptions.ProductNotFoundException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_KEY = "error";

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientFunds(InsufficientFundsException ex) {
        return ResponseEntity.status(402).body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientStock(InsufficientStockException ex) {
        return ResponseEntity.status(409).body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(400).body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(OrderAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(OrderAccessDeniedException ex) {
        return ResponseEntity.status(403).body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOrderNotFound(OrderNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(OrderServiceException.class)
    public ResponseEntity<Map<String, String>> handleServiceError(OrderServiceException ex) {
        return ResponseEntity.status(502).body(Map.of(ERROR_KEY, ex.getMessage()));
    }
}
