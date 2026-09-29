package pe.com.salon.salongestionapi.auth.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class TokenInfoResponse {

    public TokenInfoResponse() {
    }

    public TokenInfoResponse(Long timeRemainingSeconds, String token, Long timeRemainingMs, Long expirationTimeMs,
            Boolean isExpired) {
        this.timeRemainingSeconds = timeRemainingSeconds;
        this.token = token;
        this.timeRemainingMs = timeRemainingMs;
        this.expirationTimeMs = expirationTimeMs;
        this.isExpired = isExpired;
    }

    public static class TokenInfoResponseBuilder {
        private Long timeRemainingSeconds;
        private String token;
        private Long timeRemainingMs;
        private Long expirationTimeMs;
        private Boolean isExpired;

        public TokenInfoResponseBuilder timeRemainingSeconds(Long timeRemainingSeconds) {
            this.timeRemainingSeconds = timeRemainingSeconds;
            return this;
        }

        public TokenInfoResponseBuilder token(String token) {
            this.token = token;
            return this;
        }

        public TokenInfoResponseBuilder timeRemainingMs(Long timeRemainingMs) {
            this.timeRemainingMs = timeRemainingMs;
            return this;
        }

        public TokenInfoResponseBuilder expirationTimeMs(Long expirationTimeMs) {
            this.expirationTimeMs = expirationTimeMs;
            return this;
        }

        public TokenInfoResponseBuilder isExpired(Boolean isExpired) {
            this.isExpired = isExpired;
            return this;
        }

        public TokenInfoResponse build() {
            return new TokenInfoResponse(timeRemainingSeconds, token, timeRemainingMs, expirationTimeMs, isExpired);
        }
    }

    public static TokenInfoResponseBuilder builder() {
        return new TokenInfoResponseBuilder();
    }

    public Long getTimeRemainingSeconds() {
        return timeRemainingSeconds;
    }

    public void setTimeRemainingSeconds(Long timeRemainingSeconds) {
        this.timeRemainingSeconds = timeRemainingSeconds;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getTimeRemainingMs() {
        return timeRemainingMs;
    }

    public void setTimeRemainingMs(Long timeRemainingMs) {
        this.timeRemainingMs = timeRemainingMs;
    }

    public Long getExpirationTimeMs() {
        return expirationTimeMs;
    }

    public void setExpirationTimeMs(Long expirationTimeMs) {
        this.expirationTimeMs = expirationTimeMs;
    }

    public Boolean getIsExpired() {
        return isExpired;
    }

    public void setIsExpired(Boolean isExpired) {
        this.isExpired = isExpired;
    }

    private Long timeRemainingSeconds; // Tiempo restante en segundos
    private String token; // Token renovado (opcional)
    private Long timeRemainingMs; // Tiempo restante en milisegundos
    private Long expirationTimeMs; // Tiempo total de expiración configurado
    private Boolean isExpired; // Si el token ya expiró
}
