package pe.com.salon.salongestionapi.fidelizacion.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.com.salon.salongestionapi.catalogo.entity.Servicio;

@Entity
@Table(name = "cartilla_fidelizacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartillaFidelizacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @Column(name = "meta_sellos", nullable = false)
    private Integer metaSellos;

    @Column(name = "descuento_premio", nullable = false)
    private Double descuentoPremio; // 100 for 100% free

    @Column(nullable = false)
    private Boolean estado;
}
