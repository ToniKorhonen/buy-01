package service.product.exception;

public class JwtTokenValidationException extends RuntimeException {
    public JwtTokenValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public JwtTokenValidationException(String message) {
        super(message);
    }
}

