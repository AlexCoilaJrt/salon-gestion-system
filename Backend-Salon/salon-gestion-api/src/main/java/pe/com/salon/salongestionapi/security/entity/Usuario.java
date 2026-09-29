package pe.com.salon.salongestionapi.security.entity;

import jakarta.persistence.*;
import lombok.*;
import pe.com.salon.salongestionapi.roles.entity.Role;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 100, unique = true)
    private String email;

    @Column(nullable = false)
    private Boolean estado = true;

    @Column(name = "last_login")
    private java.time.LocalDateTime lastLogin;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Transient
    public Boolean getActive() {
        return this.estado;
    }

    // Relación opcional con Empleado
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", unique = true)
    private Empleado empleado;

    // Relación Muchos a Muchos con Role (tomando tu entidad Role)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Role> roles = new HashSet<>();

    // Métodos utilitarios para compatibilidad con JwtService
    @Transient
    public String getSexo() {
        return "N/A"; // Stub, o podrías traerlo de Usuario si existiera
    }

    @Transient
    public List<String> getPermissionNames() {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream()
                .filter(role -> role.getPermissions() != null)
                .flatMap(role -> role.getPermissions().stream())
                .filter(permission -> Boolean.TRUE.equals(permission.getActive()))
                .map(pe.com.salon.salongestionapi.permissions.entity.Permission::getName)
                .distinct()
                .collect(Collectors.toList());
    }
}
