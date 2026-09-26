package pe.com.salon.salongestionapi.finanzas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gasto_caja_chica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GastoCajaChica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Column(name = "fecha_gasto", nullable = false)
    private LocalDateTime fechaGasto;

    @Column(nullable = false)
    private Boolean estado = true; // true = Activo, false = Anulado
}
