package service.order.exceptions;

public class JwtProcessingException extends RuntimeException {
    public JwtProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

