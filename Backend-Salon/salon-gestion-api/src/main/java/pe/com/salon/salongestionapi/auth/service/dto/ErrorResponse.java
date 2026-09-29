package pe.com.salon.salongestionapi.auth.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ErrorResponse {
    public ErrorResponse() {
    }

    public ErrorResponse(boolean success, String message, Integer remainingAttempts, boolean blocked,
            LocalDateTime unblockTime, LocalDateTime timestamp) {
        this.success = success;
        this.message = message;
        this.remainingAttempts = remainingAttempts;
        this.blocked = blocked;
        this.unblockTime = unblockTime;
        this.timestamp = timestamp;
    }

    public static class ErrorResponseBuilder {
        private boolean success;
        private String message;
        private Integer remainingAttempts;
        private boolean blocked = false;
        private LocalDateTime unblockTime;
        private LocalDateTime timestamp = LocalDateTime.now();

        public ErrorResponseBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public ErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorResponseBuilder remainingAttempts(Integer remainingAttempts) {
            this.remainingAttempts = remainingAttempts;
            return this;
        }

        public ErrorResponseBuilder blocked(boolean blocked) {
            this.blocked = blocked;
            return this;
        }

        public ErrorResponseBuilder unblockTime(LocalDateTime unblockTime) {
            this.unblockTime = unblockTime;
            return this;
        }

        public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(success, message, remainingAttempts, blocked, unblockTime, timestamp);
        }
    }

    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getRemainingAttempts() {
        return remainingAttempts;
    }

    public void setRemainingAttempts(Integer remainingAttempts) {
        this.remainingAttempts = remainingAttempts;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public LocalDateTime getUnblockTime() {
        return unblockTime;
    }

    public void setUnblockTime(LocalDateTime unblockTime) {
        this.unblockTime = unblockTime;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    private boolean success;
    private String message;
    private Integer remainingAttempts;

    @Builder.Default
    private boolean blocked = false;
    private LocalDateTime unblockTime;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
