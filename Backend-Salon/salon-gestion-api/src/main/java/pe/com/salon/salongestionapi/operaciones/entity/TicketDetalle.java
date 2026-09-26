package pe.com.salon.salongestionapi.operaciones.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.salon.salongestionapi.catalogo.entity.Producto;
import pe.com.salon.salongestionapi.catalogo.entity.Servicio;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;

import java.math.BigDecimal;

@Entity
@Table(name = "ticket_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    // Relaciones para saber QUÉ se vendió
    // Al menos uno debe estar presente (Servicio o Producto)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    // Relación clave para las comisiones (Saber QUIÉN hizo el servicio)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;
}
