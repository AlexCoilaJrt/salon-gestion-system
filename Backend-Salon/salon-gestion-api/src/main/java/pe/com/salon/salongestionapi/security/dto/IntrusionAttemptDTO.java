package pe.com.salon.salongestionapi.security.dto;

import java.time.LocalDateTime;

public class IntrusionAttemptDTO {

    private String ipAddress;
    private String username;
    private Integer totalIntentos;
    private LocalDateTime ultimoIntento;
    private LocalDateTime tiempoDesbloqueo;
    private String motivoBloqueo;

    public IntrusionAttemptDTO() {
    }

    public IntrusionAttemptDTO(String ipAddress, String username, Integer totalIntentos, LocalDateTime ultimoIntento,
            LocalDateTime tiempoDesbloqueo, String motivoBloqueo) {
        this.ipAddress = ipAddress;
        this.username = username;
        this.totalIntentos = totalIntentos;
        this.ultimoIntento = ultimoIntento;
        this.tiempoDesbloqueo = tiempoDesbloqueo;
        this.motivoBloqueo = motivoBloqueo;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getTotalIntentos() {
        return totalIntentos;
    }

    public void setTotalIntentos(Integer totalIntentos) {
        this.totalIntentos = totalIntentos;
    }

    public LocalDateTime getUltimoIntento() {
        return ultimoIntento;
    }

    public void setUltimoIntento(LocalDateTime ultimoIntento) {
        this.ultimoIntento = ultimoIntento;
    }

    public LocalDateTime getTiempoDesbloqueo() {
        return tiempoDesbloqueo;
    }

    public void setTiempoDesbloqueo(LocalDateTime tiempoDesbloqueo) {
        this.tiempoDesbloqueo = tiempoDesbloqueo;
    }

    public String getMotivoBloqueo() {
        return motivoBloqueo;
    }

    public void setMotivoBloqueo(String motivoBloqueo) {
        this.motivoBloqueo = motivoBloqueo;
    }

    public static class IntrusionAttemptDTOBuilder {
        private String ipAddress;
        private String username;
        private Integer totalIntentos;
        private LocalDateTime ultimoIntento;
        private LocalDateTime tiempoDesbloqueo;
        private String motivoBloqueo;

        IntrusionAttemptDTOBuilder() {
        }

        public IntrusionAttemptDTOBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public IntrusionAttemptDTOBuilder username(String username) {
            this.username = username;
            return this;
        }

        public IntrusionAttemptDTOBuilder totalIntentos(Integer totalIntentos) {
            this.totalIntentos = totalIntentos;
            return this;
        }

        public IntrusionAttemptDTOBuilder ultimoIntento(LocalDateTime ultimoIntento) {
            this.ultimoIntento = ultimoIntento;
            return this;
        }

        public IntrusionAttemptDTOBuilder tiempoDesbloqueo(LocalDateTime tiempoDesbloqueo) {
            this.tiempoDesbloqueo = tiempoDesbloqueo;
            return this;
        }

        public IntrusionAttemptDTOBuilder motivoBloqueo(String motivoBloqueo) {
            this.motivoBloqueo = motivoBloqueo;
            return this;
        }

        public IntrusionAttemptDTO build() {
            return new IntrusionAttemptDTO(ipAddress, username, totalIntentos, ultimoIntento, tiempoDesbloqueo,
                    motivoBloqueo);
        }
    }

    public static IntrusionAttemptDTOBuilder builder() {
        return new IntrusionAttemptDTOBuilder();
    }
}
