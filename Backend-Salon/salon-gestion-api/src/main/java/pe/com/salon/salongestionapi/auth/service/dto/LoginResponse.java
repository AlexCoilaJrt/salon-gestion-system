package pe.com.salon.salongestionapi.auth.service.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String type; // "Bearer"
    private Long userId;
    private Long idPersonal; // ID de Empleado (el que se usa para vínculos)
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String sexo;
    private String telefono;
    private String dni;
    private Set<String> roles;
    private Set<String> permissions;
    private Long sessionTimeoutMs; // Timeout de sesión en milisegundos

    // ---------------------------------------------------------
    // Campos para selección de sucursal (Comentados temporalmente 
    // porque el módulo de sucursales aún no existe en el Salón)
    // ---------------------------------------------------------
    // private Boolean requiresSucursalSelection;
    // private java.util.List<Object> sucursalesDisponibles;
    // private Long sucursalId;
    // private String sucursalNombre;
}
