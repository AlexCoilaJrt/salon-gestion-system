package pe.com.salon.salongestionapi.exception;

import java.time.LocalDateTime;

public class AuthException extends RuntimeException {
    
    private int remainingAttempts;
    private boolean isBlocked;
    private LocalDateTime unblockTime;

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, int remainingAttempts, boolean isBlocked, LocalDateTime unblockTime) {
        super(message);
        this.remainingAttempts = remainingAttempts;
        this.isBlocked = isBlocked;
        this.unblockTime = unblockTime;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public LocalDateTime getUnblockTime() {
        return unblockTime;
    }
}
