package pe.com.salon.salongestionapi.perfil_modulo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.com.salon.salongestionapi.roles.entity.Role;

import java.time.LocalDateTime;

@Entity
@Table(name = "perfil_modulo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilModulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    /**
     * Columna legacy (FK al Modulo). Mantenida para compatibilidad con BD.
     */
    @Column(name = "modulo_id")
    private Long moduloId;

    /**
     * Columna legacy (FK al Role). MenuService la usa como profileId = rol.getId().
     */
    @Column(name = "profile_id")
    private Long profileId;

    /** Relación JPA con el Role dueño de este perfil */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", insertable = false, updatable = false)
    private Role role;

    /** Relación JPA con el Modulo */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modulo_id", insertable = false, updatable = false)
    private Modulo modulo;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isDeleted == null) isDeleted = false;
        if (createdBy == null) createdBy = "SYSTEM";
    }
}
