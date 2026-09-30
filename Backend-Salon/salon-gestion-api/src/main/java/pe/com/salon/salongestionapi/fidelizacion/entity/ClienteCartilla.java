package pe.com.salon.salongestionapi.fidelizacion.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.com.salon.salongestionapi.operaciones.entity.Cliente;

import java.time.LocalDateTime;

@Entity
@Table(name = "cliente_cartilla")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteCartilla {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartilla_fidelizacion_id", nullable = false)
    private CartillaFidelizacion cartillaFidelizacion;

    @Column(name = "sellos_actuales", nullable = false)
    private Integer sellosActuales;

    @Column(name = "fecha_ultima_actualizacion")
    private LocalDateTime fechaUltimaActualizacion;

    @Column(nullable = false)
    private Boolean completada; // True when sellos >= meta

    @Column(nullable = false)
    private Boolean canjeada; // True when the client redeems the reward
}
