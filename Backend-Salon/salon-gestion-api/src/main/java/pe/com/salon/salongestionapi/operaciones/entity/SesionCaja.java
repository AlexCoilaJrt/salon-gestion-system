package pe.com.salon.salongestionapi.operaciones.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.salon.salongestionapi.security.entity.Usuario;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuarioApertura;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "monto_inicial", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoInicial = BigDecimal.ZERO;
    
    // Lo que el sistema calcula que debería haber (Inicial + Ventas - Egresos)
    @Column(name = "monto_esperado", precision = 10, scale = 2)
    private BigDecimal montoEsperado;

    // Lo que el usuario contó físicamente en la caja al cerrar
    @Column(name = "monto_declarado", precision = 10, scale = 2)
    private BigDecimal montoDeclarado;

    // La diferencia: Esperado vs Declarado (Positivo sobra, Negativo falta)
    @Column(name = "descuadre", precision = 10, scale = 2)
    private BigDecimal descuadre;

    @Column(length = 500)
    private String observaciones;

    // true = ABIERTA, false = CERRADA
    @Column(nullable = false)
    private Boolean estado = true;
}
