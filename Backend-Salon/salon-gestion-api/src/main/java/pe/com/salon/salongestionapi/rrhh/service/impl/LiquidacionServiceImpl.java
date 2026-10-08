package pe.com.salon.salongestionapi.rrhh.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.salon.salongestionapi.exception.ResourceNotFoundException;
import pe.com.salon.salongestionapi.rrhh.dto.LiquidacionRequest;
import pe.com.salon.salongestionapi.rrhh.dto.LiquidacionResponse;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;
import pe.com.salon.salongestionapi.rrhh.entity.EstadoLiquidacion;
import pe.com.salon.salongestionapi.rrhh.entity.Liquidacion;
import pe.com.salon.salongestionapi.rrhh.repository.EmpleadoRepository;
import pe.com.salon.salongestionapi.rrhh.repository.LiquidacionRrhhRepository;
import pe.com.salon.salongestionapi.rrhh.service.LiquidacionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LiquidacionServiceImpl implements LiquidacionService {

    private final LiquidacionRrhhRepository liquidacionRepository;
    private final EmpleadoRepository empleadoRepository;

    @Override
    @Transactional
    public LiquidacionResponse crearLiquidacion(LiquidacionRequest request) {
        Empleado empleado = empleadoRepository.findById(request.getEmpleadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));

        Liquidacion liquidacion = new Liquidacion();
        liquidacion.setEmpleado(empleado);
        liquidacion.setFechaInicio(request.getFechaInicio());
        liquidacion.setFechaFin(request.getFechaFin());
        liquidacion.setTotalVentas(request.getTotalVentas());
        liquidacion.setTotalComision(request.getTotalComision());
        liquidacion.setSueldoFijoProporcional(request.getSueldoFijoProporcional());
        liquidacion.setDescuentos(request.getDescuentos());
        liquidacion.setTotalAPagar(request.getTotalAPagar());
        liquidacion.setEstado(request.getEstado());
        liquidacion.setDiasAsistidos(request.getDiasAsistidos());
        liquidacion.setMontoRetenido(request.getMontoRetenido() != null ? request.getMontoRetenido() : BigDecimal.ZERO);
        liquidacion.setMontoLiberado(request.getMontoLiberado() != null ? request.getMontoLiberado() : BigDecimal.ZERO);
        liquidacion.setPenalidadAbandono(request.getPenalidadAbandono() != null ? request.getPenalidadAbandono() : BigDecimal.ZERO);
        liquidacion.setEsCese(request.getEsCese() != null ? request.getEsCese() : false);

        Liquidacion guardada = liquidacionRepository.save(liquidacion);
        return mapToResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LiquidacionResponse> obtenerPorEmpleado(Long empleadoId) {
        return liquidacionRepository.findByEmpleadoId(empleadoId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LiquidacionResponse> obtenerTodas() {
        return liquidacionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
