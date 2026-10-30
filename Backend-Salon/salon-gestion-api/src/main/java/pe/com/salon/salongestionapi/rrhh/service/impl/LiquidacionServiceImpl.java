package pe.com.salon.salongestionapi.rrhh.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.salon.salongestionapi.exception.ResourceNotFoundException;
import pe.com.salon.salongestionapi.rrhh.dto.LiquidacionRequest;
import pe.com.salon.salongestionapi.rrhh.dto.LiquidacionResponse;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;
import pe.com.salon.salongestionapi.rrhh.entity.EstadoLiquidacion;
import pe.com.salon.salongestionapi.rrhh.entity.Liquidacion;
import pe.com.salon.salongestionapi.rrhh.repository.EmpleadoRepository;
import pe.com.salon.salongestionapi.rrhh.repository.LiquidacionRrhhRepository;
import pe.com.salon.salongestionapi.rrhh.service.IncentivoService;
import pe.com.salon.salongestionapi.rrhh.service.LiquidacionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LiquidacionServiceImpl implements LiquidacionService {

    private final LiquidacionRrhhRepository liquidacionRepository;
    private final EmpleadoRepository empleadoRepository;
    private final JdbcTemplate jdbcTemplate;
    private final IncentivoService incentivoService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            jdbcTemplate.execute("ALTER TABLE liquidaciones_rrhh DROP CONSTRAINT liquidaciones_rrhh_estado_check");
        } catch (Exception e) {
            // Ignorar si la restricción no existe
        }
    }

    @Override
    @Transactional
    public LiquidacionResponse crearLiquidacion(LiquidacionRequest request) {
        Empleado empleado = empleadoRepository.findById(request.getEmpleadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));

        // Si ya existe una liquidación del mismo empleado y periodo, se recalcula en lugar de duplicarla.
        // Sólo las PENDIENTE se actualizan: una vez pagada (o retenida) queda congelada.
        Liquidacion existente = liquidacionRepository
                .findFirstByEmpleadoIdAndFechaInicioAndFechaFin(empleado.getId(), request.getFechaInicio(), request.getFechaFin())
                .orElse(null);
        if (existente != null && existente.getEstado() != EstadoLiquidacion.PENDIENTE) {
            return mapToResponse(existente);
        }

        Liquidacion liquidacion = existente != null ? existente : new Liquidacion();
        liquidacion.setEmpleado(empleado);
        liquidacion.setFechaInicio(request.getFechaInicio());
        liquidacion.setFechaFin(request.getFechaFin());
        liquidacion.setTotalVentas(request.getTotalVentas());
        liquidacion.setTotalComision(request.getTotalComision());
        liquidacion.setSueldoFijoProporcional(request.getSueldoFijoProporcional());
        liquidacion.setDescuentos(request.getDescuentos());
        liquidacion.setTotalAPagar(request.getTotalAPagar());
        if (existente == null) {
            liquidacion.setEstado(request.getEstado());
        }
        liquidacion.setDiasAsistidos(request.getDiasAsistidos());
        liquidacion.setMontoRetenido(request.getMontoRetenido() != null ? request.getMontoRetenido() : BigDecimal.ZERO);
        liquidacion.setMontoLiberado(request.getMontoLiberado() != null ? request.getMontoLiberado() : BigDecimal.ZERO);
        liquidacion.setPenalidadAbandono(request.getPenalidadAbandono() != null ? request.getPenalidadAbandono() : BigDecimal.ZERO);
        liquidacion.setEsCese(request.getEsCese() != null ? request.getEsCese() : false);

        Liquidacion guardada = liquidacionRepository.save(liquidacion);
        return mapToResponse(guardada);
    }

    @Override
    @Transactional
    public List<LiquidacionResponse> obtenerPorEmpleado(Long empleadoId) {
        return liquidacionRepository.findByEmpleadoId(empleadoId).stream()
                .map(this::recalcularSiPendiente)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<LiquidacionResponse> obtenerTodas() {
        return liquidacionRepository.findAll().stream()
                .map(this::recalcularSiPendiente)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Una liquidación PENDIENTE refleja siempre las ventas actuales del periodo (nuevas ventas, anulaciones).
     * Las PAGADAS/RETENIDAS/ANULADAS quedan congeladas con el monto con que se cerraron.
     */
    private Liquidacion recalcularSiPendiente(Liquidacion liquidacion) {
        if (liquidacion.getEstado() != EstadoLiquidacion.PENDIENTE) {
            return liquidacion;
        }
        LiquidacionResponse calculo = incentivoService.calcularLiquidacionEmpleado(
                liquidacion.getEmpleado().getId(), liquidacion.getFechaInicio(), liquidacion.getFechaFin());

        BigDecimal descuentos = liquidacion.getDescuentos() != null ? liquidacion.getDescuentos() : BigDecimal.ZERO;
        BigDecimal total = calculo.getSueldoFijoProporcional().add(calculo.getTotalComision()).subtract(descuentos);

        boolean cambio = !mismoMonto(liquidacion.getTotalVentas(), calculo.getTotalVentas())
                || !mismoMonto(liquidacion.getTotalComision(), calculo.getTotalComision())
                || !mismoMonto(liquidacion.getSueldoFijoProporcional(), calculo.getSueldoFijoProporcional())
                || !mismoMonto(liquidacion.getTotalAPagar(), total);
        if (!cambio) {
            return liquidacion;
        }

        liquidacion.setTotalVentas(calculo.getTotalVentas());
        liquidacion.setTotalComision(calculo.getTotalComision());
        liquidacion.setSueldoFijoProporcional(calculo.getSueldoFijoProporcional());
        liquidacion.setDiasAsistidos(calculo.getDiasAsistidos());
        liquidacion.setTotalAPagar(total);
        return liquidacionRepository.save(liquidacion);
    }

    private boolean mismoMonto(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    @Override
    @Transactional
    public LiquidacionResponse actualizarEstado(Long id, String estado) {
        Liquidacion liquidacion = liquidacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidación no encontrada"));
        
        liquidacion.setEstado(EstadoLiquidacion.valueOf(estado));
        return mapToResponse(liquidacionRepository.save(liquidacion));
    }

    private LiquidacionResponse mapToResponse(Liquidacion l) {
        LiquidacionResponse r = new LiquidacionResponse();
        r.setId(l.getId());
        r.setEmpleadoId(l.getEmpleado().getId());
        r.setEmpleadoNombreCompleto(l.getEmpleado().getNombres() + " " + l.getEmpleado().getApellidos());
        r.setFechaInicio(l.getFechaInicio());
        r.setFechaFin(l.getFechaFin());
        r.setTotalVentas(l.getTotalVentas());
        r.setTotalComision(l.getTotalComision());
        r.setSueldoFijoProporcional(l.getSueldoFijoProporcional());
        r.setDescuentosAdelantos(l.getDescuentos());
        r.setTotalPagar(l.getTotalAPagar());
        r.setEstado(l.getEstado().name());
        r.setDiasAsistidos(l.getDiasAsistidos());
        r.setMontoRetenido(l.getMontoRetenido());
        r.setMontoLiberado(l.getMontoLiberado());
        r.setPenalidadAbandono(l.getPenalidadAbandono());
        r.setEsCese(l.getEsCese());
        r.setFechaRegistro(l.getFechaRegistro());
        return r;
    }
}
