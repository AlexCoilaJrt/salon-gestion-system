package pe.com.salon.salongestionapi.perfil_modulo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.com.salon.salongestionapi.menu.entity.AccesoMain;

@Entity
@Table(name = "modulo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Modulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_modulo")
    private Long id;

    @Column(name = "code")
    private String code;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "icon")
    private String icon;

    @Column(name = "name")
    private String name;

    @Column(name = "order_number")
    private Integer orderNumber;

    /** Relaciona el módulo con un portal de acceso (REHAB, SALON, etc.) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_acceso")
    private AccesoMain acceso;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
