package pe.com.salon.salongestionapi.finanzas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.operaciones.entity.TicketDetalle;
import pe.com.salon.salongestionapi.rrhh.entity.IncentivoComision;
import pe.com.salon.salongestionapi.rrhh.entity.TipoComision;
import pe.com.salon.salongestionapi.rrhh.repository.IncentivoComisionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Congela (snapshot) la comision de cada linea de ticket en el momento del cobro.
 * Regla: lo ganado en una venta cobrada no cambia aunque luego cambien los porcentajes,
 * la modalidad de pago del empleado o los incentivos.
 */
@Service
@RequiredArgsConstructor
public class ComisionTicketService {

    private final MotorComisionService motorComisionService;
    private final IncentivoComisionRepository incentivoRepository;

    /** Calcula y guarda comisionMonto / comisionPorcentaje en cada detalle del ticket. */
    public void congelarComision(List<TicketDetalle> detalles, LocalDateTime fechaVenta) {
        if (detalles == null || detalles.isEmpty()) {
            return;
        }
        List<IncentivoComision> incentivos = incentivoRepository.findActiveIncentivos(fechaVenta);
        BigDecimal porcentajeExtra = BigDecimal.ZERO;
        BigDecimal montoExtra = BigDecimal.ZERO;
        for (IncentivoComision inc : incentivos) {
            if (inc.getTipoIncentivo() == TipoComision.PORCENTAJE) {
                porcentajeExtra = porcentajeExtra.add(inc.getValor());
            } else {
                montoExtra = montoExtra.add(inc.getValor());
            }
        }

        for (TicketDetalle detalle : detalles) {
            if (detalle.getEmpleado() == null || detalle.getSubtotal() == null) {
                detalle.setComisionMonto(BigDecimal.ZERO);
                detalle.setComisionPorcentaje(BigDecimal.ZERO);
                continue;
            }
            Long especialidadId = null;
            if (detalle.getServicio() != null && detalle.getServicio().getEspecialidadRequerida() != null) {
                especialidadId = detalle.getServicio().getEspecialidadRequerida().getId();
            }

            BigDecimal comision = motorComisionService.calcularComision(
                    detalle.getServicio(),
                    detalle.getEmpleado().getId(),
                    especialidadId,
                    detalle.getSubtotal(),
                    fechaVenta.toLocalDate());

            if (comision.compareTo(BigDecimal.ZERO) > 0) {
                if (porcentajeExtra.compareTo(BigDecimal.ZERO) > 0) {
                    comision = comision.add(detalle.getSubtotal().multiply(porcentajeExtra)
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
                }
                comision = comision.add(montoExtra);
            }

            BigDecimal porcentaje = BigDecimal.ZERO;
            if (detalle.getSubtotal().compareTo(BigDecimal.ZERO) > 0) {
                porcentaje = comision.multiply(new BigDecimal("100"))
                        .divide(detalle.getSubtotal(), 2, RoundingMode.HALF_UP);
            }
            detalle.setComisionMonto(comision);
            detalle.setComisionPorcentaje(porcentaje);
        }
    }
}
