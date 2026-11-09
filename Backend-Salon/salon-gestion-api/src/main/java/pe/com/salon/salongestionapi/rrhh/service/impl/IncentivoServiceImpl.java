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
import pe.com.salon.salongestionapi.rrhh.dto.MonitorComisionPageResponse;
import java.util.Collections;
import pe.com.salon.salongestionapi.rrhh.dto.DetalleLiquidacionResponse;
import pe.com.salon.salongestionapi.rrhh.dto.MonitorComisionResponse.IncentivoAplicado;
import pe.com.salon.salongestionapi.operaciones.entity.TicketDetalle;
import pe.com.salon.salongestionapi.finanzas.service.MotorComisionService;

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
    private final MotorComisionService motorComisionService;
    private final pe.com.salon.salongestionapi.rrhh.repository.AdelantoRepository adelantoRepository;

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
        return getMonitorComisiones(null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitorComisionResponse> getMonitorComisiones(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDate fInicio = fechaInicio != null ? fechaInicio : LocalDate.now();
        LocalDate fFin = fechaFin != null ? fechaFin : fInicio;

        LocalDateTime inicioRango = fInicio.atStartOfDay();
        LocalDateTime finRango = fFin.atTime(23, 59, 59);

        List<Empleado> empleadosActivos = empleadoRepository.findAll().stream()
                .filter(e -> e.getEstado() != null && e.getEstado())
                .collect(Collectors.toList());

        List<IncentivoComision> incentivosActivos = incentivoRepository.findActiveIncentivos(finRango);

        List<MonitorComisionResponse> monitorList = new ArrayList<>();
        for (Empleado empleado : empleadosActivos) {
            monitorList.add(construirMonitorEmpleado(empleado, fFin, inicioRango, finRango, incentivosActivos));
        }
        return monitorList;
    }

    @Override
    @Transactional(readOnly = true)
    public MonitorComisionPageResponse getMonitorComisionesPaginado(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String search,
            int page,
            int size) {
        LocalDate fInicio = fechaInicio != null ? fechaInicio : LocalDate.now();
        LocalDate fFin = fechaFin != null ? fechaFin : fInicio;

        LocalDateTime inicioRango = fInicio.atStartOfDay();
        LocalDateTime finRango = fFin.atTime(23, 59, 59);

        List<Empleado> empleadosActivos = empleadoRepository.findAll().stream()
                .filter(e -> e.getEstado() != null && e.getEstado())
                .collect(Collectors.toList());

        List<IncentivoComision> incentivosActivos = incentivoRepository.findActiveIncentivos(finRango);

        List<MonitorComisionResponse> todosLosMonitores = new ArrayList<>();
        for (Empleado empleado : empleadosActivos) {
            todosLosMonitores.add(construirMonitorEmpleado(empleado, fFin, inicioRango, finRango, incentivosActivos));
        }

        // Filtro por término de búsqueda (search)
        List<MonitorComisionResponse> filtrados;
        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();

            // Extraer posible número para búsqueda flexible por ID de orden / ticket
            String qNumero = q.replaceAll("(?i)orden|ticket|#", "").trim();
            Long searchTicketId = null;
            try {
                if (!qNumero.isEmpty()) {
                    searchTicketId = Long.parseLong(qNumero);
                }
            } catch (NumberFormatException ignored) {
            }

            final Long finalTicketId = searchTicketId;

            filtrados = todosLosMonitores.stream().filter(m -> {
                boolean matchEmpleado = m.getEmpleadoNombreCompleto() != null
                        && m.getEmpleadoNombreCompleto().toLowerCase().contains(q);
                boolean matchServicio = m.getDetallesServicios() != null && m.getDetallesServicios().stream().anyMatch(ds -> {
                    boolean matchNombre = ds.getServicioNombre() != null && ds.getServicioNombre().toLowerCase().contains(q);
                    boolean matchCategoria = ds.getCategoriaNombre() != null && ds.getCategoriaNombre().toLowerCase().contains(q);
                    boolean matchEsp = ds.getEspecialidadAplicada() != null && ds.getEspecialidadAplicada().toLowerCase().contains(q);

                    boolean matchTicket = false;
                    if (ds.getTicketId() != null) {
                        long tId = ds.getTicketId();
                        String tIdStr = String.valueOf(tId);
                        String tIdPadded = String.format("%04d", tId);
                        matchTicket = (finalTicketId != null && tId == finalTicketId)
                                || tIdStr.contains(q)
                                || tIdPadded.contains(q)
                                || ("#" + tIdPadded).contains(q)
                                || ("orden #" + tIdPadded).contains(q)
                                || (!qNumero.isEmpty() && tIdPadded.contains(qNumero))
                                || (!qNumero.isEmpty() && tIdStr.contains(qNumero));
                    }
                    return matchNombre || matchCategoria || matchEsp || matchTicket;
                });
                return matchEmpleado || matchServicio;
            }).collect(Collectors.toList());
        } else {
            filtrados = todosLosMonitores;
        }

        // Métricas globales del período sobre la lista completa filtrada
        BigDecimal totalVentasPeriodo = filtrados.stream()
                .map(m -> m.getVentasHoy() != null ? m.getVentasHoy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalComisionesPeriodo = filtrados.stream()
                .map(m -> m.getComisionesGanadasHoy() != null ? m.getComisionesGanadasHoy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long empleadosConVentas = filtrados.stream()
                .filter(m -> m.getVentasHoy() != null && m.getVentasHoy().compareTo(BigDecimal.ZERO) > 0)
                .count();

        long totalElements = filtrados.size();
        int safeSize = size > 0 ? size : 10;
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        if (totalPages == 0) totalPages = 1;
        int safePage = Math.max(0, page);

        int fromIndex = safePage * safeSize;
        List<MonitorComisionResponse> contentPaginado;
        if (fromIndex >= filtrados.size()) {
            contentPaginado = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, filtrados.size());
            contentPaginado = filtrados.subList(fromIndex, toIndex);
        }

        return MonitorComisionPageResponse.builder()
                .content(contentPaginado)
                .pageNumber(safePage)
                .pageSize(safeSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .isLast(safePage >= totalPages - 1)
                .totalVentasPeriodo(totalVentasPeriodo)
                .totalComisionesPeriodo(totalComisionesPeriodo)
                .empleadosConVentas(empleadosConVentas)
                .totalEmpleados(totalElements)
                .build();
    }

    private MonitorComisionResponse construirMonitorEmpleado(
            Empleado empleado,
            LocalDate fFin,
            LocalDateTime inicioRango,
            LocalDateTime finRango,
            List<IncentivoComision> incentivosActivos) {
        MonitorComisionResponse response = new MonitorComisionResponse();
        response.setEmpleadoId(empleado.getId());
        response.setEmpleadoNombreCompleto(empleado.getNombres() + " " + empleado.getApellidos());

        // Determinar Asistencia
        boolean asistenciaActiva = false;
        LocalDate fechaAsistencia = fFin.isEqual(LocalDate.now()) ? LocalDate.now() : fFin;
        java.util.Optional<RegistroAsistencia> asistenciaOpt = asistenciaRepository
                .findByEmpleadoIdAndFecha(empleado.getId(), fechaAsistencia);
        if (asistenciaOpt.isPresent()) {
            RegistroAsistencia asis = asistenciaOpt.get();
            if (fechaAsistencia.isEqual(LocalDate.now())) {
                asistenciaActiva = asis.getHoraEntrada() != null && asis.getHoraSalida() == null;
            } else {
                asistenciaActiva = asis.getHoraEntrada() != null;
            }
        }
        response.setAsistenciaActiva(asistenciaActiva);
        response.setEnTurno(asistenciaActiva);

        // Determinar Comisión Base
        BigDecimal basePorc = BigDecimal.ZERO;
        BigDecimal baseMonto = BigDecimal.ZERO;

        if (empleado.getEspecialidades() != null) {
            for (pe.com.salon.salongestionapi.rrhh.entity.Especialidad esp : empleado.getEspecialidades()) {
                if (esp.getEstado()) {
                    if ("FIJO".equalsIgnoreCase(esp.getTipoPago())
                            || "Sueldo Fijo Mensual".equalsIgnoreCase(esp.getTipoPago())) {
                        if (esp.getMontoFijo() != null) {
                            baseMonto = baseMonto.add(esp.getMontoFijo());
                        }
                    } else if ("PORCENTAJE".equalsIgnoreCase(esp.getTipoPago())
                            || "Porcentaje de Comisión".equalsIgnoreCase(esp.getTipoPago())) {
                        if (esp.getPorcentajeComision() != null) {
                            basePorc = basePorc.add(esp.getPorcentajeComision());
                        }
                    }
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

        // Calcular ventas del rango solicitado (Día, Semana o Rango)
        BigDecimal ventasPeriodo = ticketDetalleRepository.sumVentasEmpleadoEnPeriodo(empleado.getId(), inicioRango, finRango);
        response.setVentasHoy(ventasPeriodo != null ? ventasPeriodo : BigDecimal.ZERO);

        List<TicketDetalle> detallesPeriodo = ticketDetalleRepository.findByEmpleadoAndFechaRango(empleado.getId(), inicioRango, finRango);
        List<MonitorComisionResponse.DetalleServicio> detalleServicios = new ArrayList<>();
        BigDecimal comisionGanadaTotal = BigDecimal.ZERO;

        for (TicketDetalle detalle : detallesPeriodo) {
            Long especialidadId = null;
            String servicioNombre = "Producto / Otro";
            String categoriaNombre = "-";
            String especialidadAplicada = "-";

            if (detalle.getServicio() != null) {
                servicioNombre = detalle.getServicio().getNombre();
                if (detalle.getServicio().getCategoria() != null) {
                    categoriaNombre = detalle.getServicio().getCategoria().getNombre();
                }
                if (detalle.getServicio().getEspecialidadRequerida() != null) {
                    especialidadId = detalle.getServicio().getEspecialidadRequerida().getId();
                    especialidadAplicada = detalle.getServicio().getEspecialidadRequerida().getNombre();
                }
            }

            LocalDate fechaVenta = (detalle.getTicket() != null && detalle.getTicket().getFechaEmision() != null)
                    ? detalle.getTicket().getFechaEmision().toLocalDate()
                    : fFin;

            BigDecimal comisionVenta = motorComisionService.calcularComision(
                    detalle.getServicio(),
                    empleado.getId(),
                    especialidadId,
                    detalle.getSubtotal(),
                    fechaVenta);

            BigDecimal porcentajeExtra = totalPorc.subtract(basePorc);
            BigDecimal gananciaFinalLinea = comisionVenta;
            if (comisionVenta.compareTo(BigDecimal.ZERO) > 0) {
                if (porcentajeExtra.compareTo(BigDecimal.ZERO) > 0 && detalle.getSubtotal() != null) {
                    gananciaFinalLinea = gananciaFinalLinea.add(
                            detalle.getSubtotal().multiply(porcentajeExtra).divide(new BigDecimal("100"), 2,
                                    java.math.RoundingMode.HALF_UP));
                }
                gananciaFinalLinea = gananciaFinalLinea.add(totalMonto.subtract(baseMonto));
            }

            comisionGanadaTotal = comisionGanadaTotal.add(gananciaFinalLinea);

            MonitorComisionResponse.DetalleServicio ds = new MonitorComisionResponse.DetalleServicio();
            Long ticketId = (detalle.getTicket() != null) ? detalle.getTicket().getId() : null;
            ds.setTicketId(ticketId);
            ds.setServicioNombre(servicioNombre);
            ds.setCategoriaNombre(categoriaNombre);
            ds.setEspecialidadAplicada(especialidadAplicada);
            boolean esSoloFijo = basePorc.compareTo(BigDecimal.ZERO) == 0 && baseMonto.compareTo(BigDecimal.ZERO) > 0;
            ds.setTipoPago(esSoloFijo || gananciaFinalLinea.compareTo(BigDecimal.ZERO) == 0 ? "Sueldo Fijo" : "Comisión");
            ds.setPorcentajeAplicado(BigDecimal.ZERO);
            if (detalle.getSubtotal().compareTo(BigDecimal.ZERO) > 0) {
                ds.setPorcentajeAplicado(gananciaFinalLinea.multiply(new BigDecimal("100"))
                        .divide(detalle.getSubtotal(), 2, java.math.RoundingMode.HALF_UP));
            }
            ds.setPrecioCobrado(detalle.getSubtotal());
            ds.setComisionGanada(gananciaFinalLinea);
            LocalDateTime fechaHoraVenta = (detalle.getTicket() != null && detalle.getTicket().getFechaEmision() != null)
                    ? detalle.getTicket().getFechaEmision()
                    : inicioRango;
            ds.setFechaHora(fechaHoraVenta);
            detalleServicios.add(ds);
        }

        response.setComisionesGanadasHoy(comisionGanadaTotal);
        response.setDetallesServicios(detalleServicios);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<pe.com.salon.salongestionapi.rrhh.dto.LiquidacionResponse> getLiquidaciones(LocalDate fechaInicio,
            LocalDate fechaFin) {
        List<pe.com.salon.salongestionapi.rrhh.dto.LiquidacionResponse> liquidaciones = new ArrayList<>();
        List<Empleado> empleadosActivos = empleadoRepository.findAll().stream()
                .filter(e -> e.getEstado() != null && e.getEstado())
                .collect(Collectors.toList());

        LocalDateTime inicioPeriodo = fechaInicio.atStartOfDay();
        LocalDateTime finPeriodo = fechaFin.atTime(23, 59, 59);
        List<IncentivoComision> incentivosActivos = incentivoRepository.findActiveIncentivos(LocalDateTime.now());

        for (Empleado empleado : empleadosActivos) {
            liquidaciones.add(
                    calcularLiquidacion(empleado, fechaInicio, fechaFin, inicioPeriodo, finPeriodo, incentivosActivos));
        }

        return liquidaciones;
    }

    @Override
    @Transactional(readOnly = true)
    public pe.com.salon.salongestionapi.rrhh.dto.LiquidacionResponse calcularLiquidacionEmpleado(Long empleadoId,
            LocalDate fechaInicio, LocalDate fechaFin) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con id: " + empleadoId));
        List<IncentivoComision> incentivosActivos = incentivoRepository.findActiveIncentivos(LocalDateTime.now());
        return calcularLiquidacion(empleado, fechaInicio, fechaFin, fechaInicio.atStartOfDay(),
                fechaFin.atTime(23, 59, 59), incentivosActivos);
    }

    private pe.com.salon.salongestionapi.rrhh.dto.LiquidacionResponse calcularLiquidacion(Empleado empleado,
            LocalDate fechaInicio, LocalDate fechaFin,
            LocalDateTime inicioPeriodo, LocalDateTime finPeriodo,
            List<IncentivoComision> incentivosActivos) {
        pe.com.salon.salongestionapi.rrhh.dto.LiquidacionResponse response = new pe.com.salon.salongestionapi.rrhh.dto.LiquidacionResponse();
        response.setEmpleadoId(empleado.getId());
        response.setEmpleadoNombreCompleto(empleado.getNombres() + " " + empleado.getApellidos());

        // 1. Calcular Sueldo Fijo Proporcional
        BigDecimal sueldoFijo = empleado.getSueldoFijo() != null ? empleado.getSueldoFijo() : BigDecimal.ZERO;

        // Fallback: Si el sueldo fijo está en 0 en la entidad Empleado, buscar en
        // Especialidades
        if (sueldoFijo.compareTo(BigDecimal.ZERO) == 0 && empleado.getEspecialidades() != null) {
            for (pe.com.salon.salongestionapi.rrhh.entity.Especialidad esp : empleado.getEspecialidades()) {
                if (esp.getEstado() && ("FIJO".equalsIgnoreCase(esp.getTipoPago())
                        || "Sueldo Fijo Mensual".equalsIgnoreCase(esp.getTipoPago()))) {
                    if (esp.getMontoFijo() != null) {
                        sueldoFijo = sueldoFijo.add(esp.getMontoFijo());
                    }
                }
            }
        }

        LocalDate fechaIngreso = empleado.getFechaIngreso();
        BigDecimal sueldoFijoProporcional = sueldoFijo;
        long diasTrabajados = java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;

        if (fechaIngreso != null) {
            if (fechaIngreso.isAfter(fechaFin)) {
                diasTrabajados = 0;
                sueldoFijoProporcional = BigDecimal.ZERO;
            } else if (fechaIngreso.isAfter(fechaInicio)) {
                diasTrabajados = java.time.temporal.ChronoUnit.DAYS.between(fechaIngreso, fechaFin) + 1;
                long diasEnPeriodo = java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
                if (diasEnPeriodo > 0 && sueldoFijo.compareTo(BigDecimal.ZERO) > 0) {
                    sueldoFijoProporcional = sueldoFijo.multiply(new BigDecimal(diasTrabajados))
                            .divide(new BigDecimal(diasEnPeriodo), 2, java.math.RoundingMode.HALF_UP);
                }
            }
        }

        response.setDiasAsistidos((int) diasTrabajados);
        response.setSueldoFijoProporcional(sueldoFijoProporcional);

        // 2. Ventas en el periodo
        BigDecimal ventas = ticketDetalleRepository.sumVentasEmpleadoEnPeriodo(empleado.getId(), inicioPeriodo,
                finPeriodo);
        response.setTotalVentas(ventas != null ? ventas : BigDecimal.ZERO);

        // 3. Comisiones exactas por servicio (mismo cálculo que el desglose de la
        // liquidación)
        BigDecimal comisionGanadaTotal = calcularLineasLiquidacion(empleado.getId(), inicioPeriodo, finPeriodo,
                incentivosActivos)
                .stream()
                .map(DetalleLiquidacionResponse::getComision)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        response.setTotalComision(comisionGanadaTotal);

        // 4. Descuentos y Total (suma de adelantos activos en el periodo)
        BigDecimal totalAdelantos = adelantoRepository.sumAdelantosEnPeriodo(empleado.getId(), fechaInicio, fechaFin);
        response.setDescuentosAdelantos(totalAdelantos != null ? totalAdelantos : BigDecimal.ZERO);
        BigDecimal totalPagar = response.getSueldoFijoProporcional()
                .add(response.getTotalComision())
                .subtract(response.getDescuentosAdelantos());
        response.setTotalPagar(totalPagar.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : totalPagar);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DetalleLiquidacionResponse> getDetalleLiquidacion(Long empleadoId, LocalDate fechaInicio,
            LocalDate fechaFin) {
        List<IncentivoComision> incentivosActivos = incentivoRepository.findActiveIncentivos(LocalDateTime.now());
        return calcularLineasLiquidacion(empleadoId, fechaInicio.atStartOfDay(), fechaFin.atTime(23, 59, 59),
                incentivosActivos);
    }

    /**
     * Calcula, línea por línea, la comisión de cada servicio/producto vendido por
     * el empleado en el periodo.
     * Lo usan tanto el total de la liquidación como su desglose, para que siempre
     * cuadren.
     */
    private List<DetalleLiquidacionResponse> calcularLineasLiquidacion(Long empleadoId, LocalDateTime inicioPeriodo,
            LocalDateTime finPeriodo, List<IncentivoComision> incentivosActivos) {
        BigDecimal porcentajeExtra = BigDecimal.ZERO;
        BigDecimal montoFijoExtra = BigDecimal.ZERO;
        // Para ser exactos, los incentivos extra aplican si asistió.
        // Como esto es mensual, sumamos incentivos que aplicaban. Simplificamos usando
        // los incentivos actuales.
        for (IncentivoComision inc : incentivosActivos) {
            if (inc.getTipoIncentivo() == TipoComision.PORCENTAJE) {
                porcentajeExtra = porcentajeExtra.add(inc.getValor());
            } else {
                montoFijoExtra = montoFijoExtra.add(inc.getValor());
            }
        }

        List<DetalleLiquidacionResponse> lineas = new ArrayList<>();
        for (TicketDetalle detalle : ticketDetalleRepository.findByEmpleadoAndFechaRango(empleadoId, inicioPeriodo,
                finPeriodo)) {
            Long especialidadId = null;
            if (detalle.getServicio() != null && detalle.getServicio().getEspecialidadRequerida() != null) {
                especialidadId = detalle.getServicio().getEspecialidadRequerida().getId();
            }

            LocalDate fechaServicio = (detalle.getTicket() != null && detalle.getTicket().getFechaEmision() != null)
                    ? detalle.getTicket().getFechaEmision().toLocalDate()
                    : LocalDate.now();
            BigDecimal comisionVenta = motorComisionService.calcularComision(
                    detalle.getServicio(),
                    empleadoId,
                    especialidadId,
                    detalle.getSubtotal(),
                    fechaServicio);

            BigDecimal gananciaLinea = comisionVenta;
            if (comisionVenta.compareTo(BigDecimal.ZERO) > 0) {
                if (porcentajeExtra.compareTo(BigDecimal.ZERO) > 0 && detalle.getSubtotal() != null) {
                    gananciaLinea = gananciaLinea.add(
                            detalle.getSubtotal().multiply(porcentajeExtra).divide(new BigDecimal("100"), 2,
                                    java.math.RoundingMode.HALF_UP));
                }
                gananciaLinea = gananciaLinea.add(montoFijoExtra);
            }

            DetalleLiquidacionResponse linea = new DetalleLiquidacionResponse();
            if (detalle.getTicket() != null) {
                linea.setTicketId(detalle.getTicket().getId());
                linea.setFecha(detalle.getTicket().getFechaEmision());
                linea.setCliente(nombreCliente(detalle.getTicket()));
            }
            if (detalle.getServicio() != null) {
                linea.setTipo("SERVICIO");
                linea.setDescripcion(detalle.getServicio().getNombre());
            } else {
                linea.setTipo("PRODUCTO");
                linea.setDescripcion(detalle.getProducto() != null ? detalle.getProducto().getNombre() : "Producto");
            }
            linea.setCantidad(detalle.getCantidad());
            linea.setPrecioUnitario(detalle.getPrecioUnitario());
            linea.setSubtotal(detalle.getSubtotal());
            linea.setComision(gananciaLinea);
            lineas.add(linea);
        }
        return lineas;
    }

    private String nombreCliente(pe.com.salon.salongestionapi.operaciones.entity.Ticket ticket) {
        if (ticket.getNombreClienteNoRegistrado() != null && !ticket.getNombreClienteNoRegistrado().isBlank()) {
            return ticket.getNombreClienteNoRegistrado();
        }
        if (ticket.getCliente() != null) {
            String nombre = ((ticket.getCliente().getNombres() != null ? ticket.getCliente().getNombres() : "") + " "
                    + (ticket.getCliente().getApellidos() != null ? ticket.getCliente().getApellidos() : "")).trim();
            return nombre.isEmpty() ? "Público General" : nombre;
        }
        return "Público General";
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
