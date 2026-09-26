package pe.com.salon.salongestionapi.operaciones.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sesion_caja")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SesionCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "monto_inicial", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoInicial = BigDecimal.ZERO;
    
    @Column(name = "monto_final", precision = 10, scale = 2)
    private BigDecimal montoFinal;

    // true = ABIERTA, false = CERRADA
    @Column(nullable = false)
    private Boolean estado = true;
}
