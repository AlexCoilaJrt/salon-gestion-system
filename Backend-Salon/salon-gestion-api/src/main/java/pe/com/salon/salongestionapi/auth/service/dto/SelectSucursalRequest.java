package pe.com.salon.salongestionapi.auth.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class SelectSucursalRequest {
    public SelectSucursalRequest() {
    }

    public SelectSucursalRequest(Long userId, Long sucursalId, String password) {
        this.userId = userId;
        this.sucursalId = sucursalId;
        this.password = password;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSucursalId() {
        return sucursalId;
    }

    public void setSucursalId(Long sucursalId) {
        this.sucursalId = sucursalId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @NotNull(message = "El ID de usuario es obligatorio")
    private Long userId;

    @NotNull(message = "El ID de sucursal es obligatorio")
    private Long sucursalId;

    @NotBlank(message = "La contraseña es requerida")
    private String password;
}
