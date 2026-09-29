package pe.com.salon.salongestionapi.auth.exception;

/**
 * Exception thrown when security constraints are violated
 * (e.g., IP blocked, excessive failed attempts)
 */
public class SecurityException extends RuntimeException {

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}

