package service.user.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message, String retrieval) {
        super(message);
    }
}
