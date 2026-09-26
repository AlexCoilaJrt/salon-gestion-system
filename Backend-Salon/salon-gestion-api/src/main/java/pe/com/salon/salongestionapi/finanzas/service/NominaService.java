package pe.com.salon.salongestionapi.finanzas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.finanzas.dto.LiquidacionRequest;
import pe.com.salon.salongestionapi.finanzas.dto.LiquidacionResponse;
import pe.com.salon.salongestionapi.finanzas.entity.Liquidacion;
import pe.com.salon.salongestionapi.finanzas.repository.LiquidacionRepository;
import pe.com.salon.salongestionapi.operaciones.entity.TicketDetalle;
import pe.com.salon.salongestionapi.operaciones.repository.TicketDetalleRepository;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;
import pe.com.salon.salongestionapi.rrhh.repository.EmpleadoRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NominaService {

    private final LiquidacionRepository liquidacionRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TicketDetalleRepository ticketDetalleRepository;
    private final MotorComisionService motorComisionService;

    public LiquidacionResponse generarLiquidacion(LiquidacionRequest request) {
        Empleado empleado = empleadoRepository.findById(request.getEmpleadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + request.getEmpleadoId()));

        // Convertir fechas a LocalDateTime para la consulta
        LocalDateTime inicio = request.getFechaInicio().atStartOfDay();
        LocalDateTime fin = request.getFechaFin().atTime(LocalTime.MAX);

        // 1. Obtener todas las ventas del empleado en ese periodo
        List<TicketDetalle> ventasEmpleado = ticketDetalleRepository
                .findByEmpleadoAndFechaRango(empleado.getId(), inicio, fin);

        BigDecimal totalVentas = BigDecimal.ZERO;
        BigDecimal totalComisiones = BigDecimal.ZERO;

        // 2. Calcular la comisión por cada venta
        Long especialidadId = (empleado.getEspecialidad() != null) ? empleado.getEspecialidad().getId() : null;

        for (TicketDetalle venta : ventasEmpleado) {
            totalVentas = totalVentas.add(venta.getSubtotal());
            
            BigDecimal comisionVenta = motorComisionService.calcularComision(especialidadId, venta.getSubtotal());
            totalComisiones = totalComisiones.add(comisionVenta);
        }

        // 3. Crear el registro de liquidación
        Liquidacion liquidacion = new Liquidacion();
        liquidacion.setFechaInicio(request.getFechaInicio());
        liquidacion.setFechaFin(request.getFechaFin());
        liquidacion.setTotalVentas(totalVentas);
        liquidacion.setTotalComisiones(totalComisiones);
        liquidacion.setEmpleado(empleado);
        liquidacion.setPagado(false); // Nace pendiente de pago

        Liquidacion guardada = liquidacionRepository.save(liquidacion);

        return mapToResponse(guardada);
    }
    
    public LiquidacionResponse pagarLiquidacion(Long id) {
        Liquidacion liquidacion = liquidacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidacion no encontrada con id: " + id));
                
        liquidacion.setPagado(true);
        Liquidacion pagada = liquidacionRepository.save(liquidacion);
        
        return mapToResponse(pagada);
    }

    private LiquidacionResponse mapToResponse(Liquidacion liquidacion) {
        LiquidacionResponse res = new LiquidacionResponse();
        res.setId(liquidacion.getId());
        res.setFechaInicio(liquidacion.getFechaInicio());
        res.setFechaFin(liquidacion.getFechaFin());
        res.setTotalVentas(liquidacion.getTotalVentas());
        res.setTotalComisiones(liquidacion.getTotalComisiones());
        res.setEmpleadoId(liquidacion.getEmpleado().getId());
        res.setEmpleadoNombreCompleto(liquidacion.getEmpleado().getNombres() + " " + liquidacion.getEmpleado().getApellidos());
        res.setPagado(liquidacion.getPagado());
        return res;
    }
}
