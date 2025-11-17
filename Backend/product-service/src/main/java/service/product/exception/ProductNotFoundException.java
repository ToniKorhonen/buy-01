package service.product.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message, String retrieval) {
        super(message);
    }
}
