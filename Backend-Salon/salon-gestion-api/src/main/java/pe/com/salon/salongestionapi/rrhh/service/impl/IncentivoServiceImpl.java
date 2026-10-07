package pe.com.salon.salongestionapi.rrhh.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.salon.salongestionapi.rrhh.dto.IncentivoRequest;
import pe.com.salon.salongestionapi.rrhh.dto.IncentivoResponse;
import pe.com.salon.salongestionapi.rrhh.entity.IncentivoComision;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;
import pe.com.salon.salongestionapi.rrhh.entity.RegistroAsistencia;
import pe.com.salon.salongestionapi.rrhh.entity.Comision;
import pe.com.salon.salongestionapi.rrhh.entity.TipoComision;
import pe.com.salon.salongestionapi.rrhh.repository.IncentivoComisionRepository;
import pe.com.salon.salongestionapi.rrhh.repository.EmpleadoRepository;
import pe.com.salon.salongestionapi.rrhh.repository.AsistenciaRepository;
import pe.com.salon.salongestionapi.rrhh.repository.ComisionRepository;
import pe.com.salon.salongestionapi.rrhh.service.IncentivoService;
import pe.com.salon.salongestionapi.rrhh.dto.MonitorComisionResponse;
import pe.com.salon.salongestionapi.rrhh.dto.MonitorComisionResponse.IncentivoAplicado;
import pe.com.salon.salongestionapi.operaciones.entity.TicketDetalle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncentivoServiceImpl implements IncentivoService {

    private final IncentivoComisionRepository incentivoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final ComisionRepository comisionRepository;
    private final pe.com.salon.salongestionapi.operaciones.repository.TicketDetalleRepository ticketDetalleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<IncentivoResponse> findAll() {
        return incentivoRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public IncentivoResponse findById(Long id) {
        IncentivoComision incentivo = incentivoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incentivo no encontrado"));
        return mapToResponse(incentivo);
    }

    @Override
    @Transactional
    public IncentivoResponse create(IncentivoRequest request) {
        IncentivoComision incentivo = IncentivoComision.builder()
                .nombre(request.getNombre())
                .tipoIncentivo(request.getTipoIncentivo())
                .valor(request.getValor())
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin())
                .estado(request.getEstado() != null ? request.getEstado() : true)
                .build();

        return mapToResponse(incentivoRepository.save(incentivo));
    }

    @Override
    @Transactional
    public IncentivoResponse update(Long id, IncentivoRequest request) {
        IncentivoComision incentivo = incentivoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incentivo no encontrado"));

        incentivo.setNombre(request.getNombre());
        incentivo.setTipoIncentivo(request.getTipoIncentivo());
        incentivo.setValor(request.getValor());
        incentivo.setFechaInicio(request.getFechaInicio());
        incentivo.setFechaFin(request.getFechaFin());
        if (request.getEstado() != null) {
            incentivo.setEstado(request.getEstado());
        }

        return mapToResponse(incentivoRepository.save(incentivo));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        IncentivoComision incentivo = incentivoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incentivo no encontrado"));
        incentivo.setEstado(false);
        incentivoRepository.save(incentivo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncentivoResponse> getActiveIncentivos() {
        return incentivoRepository.findActiveIncentivos(LocalDateTime.now()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitorComisionResponse> getMonitorComisiones() {
        List<MonitorComisionResponse> monitorList = new ArrayList<>();
        List<Empleado> empleadosActivos = empleadoRepository.findAll().stream()
                .filter(e -> e.getEstado() != null && e.getEstado())
                .collect(Collectors.toList());
        
        List<IncentivoComision> incentivosActivos = incentivoRepository.findActiveIncentivos(LocalDateTime.now());
        
        for (Empleado empleado : empleadosActivos) {
            MonitorComisionResponse response = new MonitorComisionResponse();
            response.setEmpleadoId(empleado.getId());
            response.setEmpleadoNombreCompleto(empleado.getNombres() + " " + empleado.getApellidos());
            
            // Determinar Asistencia
            boolean asistenciaActiva = false;
            java.util.Optional<RegistroAsistencia> asistenciaOpt = asistenciaRepository.findByEmpleadoIdAndFecha(empleado.getId(), LocalDate.now());
            if (asistenciaOpt.isPresent()) {
                RegistroAsistencia asis = asistenciaOpt.get();
                if (asis.getHoraEntrada() != null && asis.getHoraSalida() == null) {
                    asistenciaActiva = true;
                }
            }
            response.setAsistenciaActiva(asistenciaActiva);
            response.setEnTurno(asistenciaActiva); // Simplificación por ahora
            
            // Determinar Comisión Base
            BigDecimal basePorc = BigDecimal.ZERO;
            BigDecimal baseMonto = BigDecimal.ZERO;
            List<Comision> comisionesEmpleado = comisionRepository.findByEmpleadoId(empleado.getId());
            for (Comision c : comisionesEmpleado) {
                if (c.getEstado()) {
                    if (c.getTipoComision() == TipoComision.PORCENTAJE) {
                        basePorc = basePorc.add(c.getValor());
                    } else {
                        baseMonto = baseMonto.add(c.getValor());
                    }
                }
            }
            response.setBasePorcentaje(basePorc);
            response.setBaseMontoFijo(baseMonto);
            
            // Aplicar Incentivos
            BigDecimal totalPorc = basePorc;
            BigDecimal totalMonto = baseMonto;
            List<IncentivoAplicado> aplicados = new ArrayList<>();
            
            if (asistenciaActiva) {
                for (IncentivoComision inc : incentivosActivos) {
                    IncentivoAplicado app = new IncentivoAplicado();
                    app.setNombre(inc.getNombre());
                    app.setTipo(inc.getTipoIncentivo().name());
                    app.setValor(inc.getValor());
                    aplicados.add(app);
                    
                    if (inc.getTipoIncentivo() == TipoComision.PORCENTAJE) {
                        totalPorc = totalPorc.add(inc.getValor());
                    } else {
                        totalMonto = totalMonto.add(inc.getValor());
                    }
                }
            }
            
            response.setIncentivosAplicados(aplicados);
            response.setTotalComisionPorcentaje(totalPorc);
            response.setTotalComisionMontoFijo(totalMonto);
            
            // Calcular ventas del dia
            LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
            LocalDateTime finDia = LocalDate.now().atTime(23, 59, 59);
            
            BigDecimal ventasHoy = ticketDetalleRepository.sumVentasEmpleadoEnPeriodo(empleado.getId(), inicioDia, finDia);
            response.setVentasHoy(ventasHoy != null ? ventasHoy : BigDecimal.ZERO);
            
            List<TicketDetalle> detallesHoy = ticketDetalleRepository.findByEmpleadoAndFechaRango(empleado.getId(), inicioDia, finDia);
            long cantidadServiciosHoy = detallesHoy.size();

            BigDecimal comisionPorcentualGanada = response.getVentasHoy().multiply(totalPorc).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal comisionGanadaTotal = comisionPorcentualGanada.add(totalMonto.multiply(new BigDecimal(cantidadServiciosHoy)));
            response.setComisionesGanadasHoy(comisionGanadaTotal);
            
            monitorList.add(response);
        }
        
        return monitorList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<pe.com.salon.salongestionapi.rrhh.dto.LiquidacionResponse> getLiquidaciones(LocalDate fechaInicio, LocalDate fechaFin) {
        List<pe.com.salon.salongestionapi.rrhh.dto.LiquidacionResponse> liquidaciones = new ArrayList<>();
        List<Empleado> empleadosActivos = empleadoRepository.findAll().stream()
                .filter(e -> e.getEstado() != null && e.getEstado())
                .collect(Collectors.toList());

        LocalDateTime inicioPeriodo = fechaInicio.atStartOfDay();
        LocalDateTime finPeriodo = fechaFin.atTime(23, 59, 59);

        for (Empleado empleado : empleadosActivos) {
            pe.com.salon.salongestionapi.rrhh.dto.LiquidacionResponse response = new pe.com.salon.salongestionapi.rrhh.dto.LiquidacionResponse();
            response.setEmpleadoId(empleado.getId());
            response.setEmpleadoNombreCompleto(empleado.getNombres() + " " + empleado.getApellidos());
            response.setSueldoFijoProporcional(BigDecimal.ZERO);

            // Ventas en el periodo
            BigDecimal ventas = ticketDetalleRepository.sumVentasEmpleadoEnPeriodo(empleado.getId(), inicioPeriodo, finPeriodo);
            response.setTotalVentas(ventas != null ? ventas : BigDecimal.ZERO);

            List<TicketDetalle> detalles = ticketDetalleRepository.findByEmpleadoAndFechaRango(empleado.getId(), inicioPeriodo, finPeriodo);
            long cantidadServicios = detalles.size();

            // Comision (usando comision base para reporte histórico)
            BigDecimal basePorc = BigDecimal.ZERO;
            BigDecimal baseMonto = BigDecimal.ZERO;
            List<Comision> comisionesEmpleado = comisionRepository.findByEmpleadoId(empleado.getId());
            for (Comision c : comisionesEmpleado) {
                if (c.getEstado()) {
                    if (c.getTipoComision() == TipoComision.PORCENTAJE) {
                        basePorc = basePorc.add(c.getValor());
                    } else {
                        baseMonto = baseMonto.add(c.getValor());
                    }
                }
            }

            BigDecimal comisionMontoFijoTotal = baseMonto.multiply(new BigDecimal(cantidadServicios));
            BigDecimal comisionTotal = response.getTotalVentas().multiply(basePorc).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP).add(comisionMontoFijoTotal);
            response.setTotalComision(comisionTotal);

            // TODO: Sumar descuentos y adelantos de egresos si aplica. Por ahora 0.
            response.setDescuentosAdelantos(BigDecimal.ZERO);

            BigDecimal totalPagar = response.getSueldoFijoProporcional().add(response.getTotalComision()).subtract(response.getDescuentosAdelantos());
            response.setTotalPagar(totalPagar);

            liquidaciones.add(response);
        }

        return liquidaciones;
    }

    private IncentivoResponse mapToResponse(IncentivoComision incentivo) {
        IncentivoResponse response = new IncentivoResponse();
        response.setId(incentivo.getId());
        response.setNombre(incentivo.getNombre());
        response.setTipoIncentivo(incentivo.getTipoIncentivo());
        response.setValor(incentivo.getValor());
        response.setFechaInicio(incentivo.getFechaInicio());
        response.setFechaFin(incentivo.getFechaFin());
        response.setEstado(incentivo.getEstado());
        response.setFechaRegistro(incentivo.getFechaRegistro());
        return response;
    }
}
