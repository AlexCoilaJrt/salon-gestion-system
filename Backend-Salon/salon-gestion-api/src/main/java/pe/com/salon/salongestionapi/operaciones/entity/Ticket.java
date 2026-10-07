package pe.com.salon.salongestionapi.operaciones.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = true, length = 30)
    private MetodoPago metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = true, length = 20, columnDefinition = "varchar(20) default 'PENDIENTE_PAGO'")
    private EstadoTicket estado = EstadoTicket.PENDIENTE_PAGO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;

    @Column(name = "nombre_cliente_no_registrado", length = 100)
    private String nombreClienteNoRegistrado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_caja_id", nullable = true)
    private SesionCaja sesionCaja;

    @Column(name = "activo", columnDefinition = "boolean default true")
    private Boolean activo = true;

    // Relación Bidireccional para gestionar los detalles desde el Ticket
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketDetalle> detalles = new ArrayList<>();
    
    public void addDetalle(TicketDetalle detalle) {
        detalles.add(detalle);
        detalle.setTicket(this);
    }
}
