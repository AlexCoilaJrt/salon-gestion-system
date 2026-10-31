package pe.com.salon.salongestionapi.operaciones.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.operaciones.dto.CierreCajaRequest;
import pe.com.salon.salongestionapi.operaciones.dto.SesionCajaRequest;
import pe.com.salon.salongestionapi.operaciones.dto.SesionCajaResponse;
import pe.com.salon.salongestionapi.operaciones.entity.SesionCaja;
import pe.com.salon.salongestionapi.operaciones.repository.SesionCajaRepository;
import pe.com.salon.salongestionapi.security.entity.Usuario;
import pe.com.salon.salongestionapi.security.repository.UsuarioRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import pe.com.salon.salongestionapi.operaciones.repository.TicketRepository;
import pe.com.salon.salongestionapi.operaciones.entity.Ticket;
import pe.com.salon.salongestionapi.operaciones.entity.TicketDetalle;
import pe.com.salon.salongestionapi.operaciones.entity.MetodoPago;
import pe.com.salon.salongestionapi.operaciones.entity.EstadoTicket;
import pe.com.salon.salongestionapi.operaciones.dto.TotalesPeriodoResponse;
import pe.com.salon.salongestionapi.operaciones.dto.MovimientosCajaResponse;
import pe.com.salon.salongestionapi.operaciones.dto.TrabajadoraResumenDTO;
import pe.com.salon.salongestionapi.operaciones.dto.TicketResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SesionCajaService {

    private static final ZoneId ZONE_PERU = ZoneId.of("America/Lima");

    private final SesionCajaRepository sesionCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TicketRepository ticketRepository;
    private final pe.com.salon.salongestionapi.operaciones.repository.EgresoRepository egresoRepository;
    private final TicketService ticketService;

    public SesionCajaResponse abrirCaja(SesionCajaRequest request) {
        if (sesionCajaRepository.findByEstadoTrue().isPresent()) {
            throw new RuntimeException("Ya existe una caja abierta actualmente. Debe cerrarla primero.");
        }

        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + request.getUsuarioId()));

        SesionCaja sesionCaja = new SesionCaja();
        sesionCaja.setUsuarioApertura(usuario);
        sesionCaja.setFechaApertura(LocalDateTime.now(ZONE_PERU));
        sesionCaja.setMontoInicial(request.getMontoInicial());
        sesionCaja.setEstado(true);

        SesionCaja guardada = sesionCajaRepository.save(sesionCaja);
        return mapToResponse(guardada);
    }

    public SesionCajaResponse cerrarCaja(CierreCajaRequest request) {
        SesionCaja sesionAbierta = sesionCajaRepository.findByEstadoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna caja abierta actualmente."));

        long pendientesCount = ticketRepository.countByEstadoAndActivoTrue(pe.com.salon.salongestionapi.operaciones.entity.EstadoTicket.PENDIENTE_PAGO);
        if (pendientesCount > 0) {
            throw new RuntimeException("No se puede cerrar la caja: Existen " + pendientesCount + 
                    " orden(es) en espera de pago pendientes. Debe cobrarlas o eliminarlas antes de cerrar caja.");
        }

        sesionAbierta.setFechaCierre(LocalDateTime.now(ZONE_PERU));
        sesionAbierta.setEstado(false);
        
        List<Ticket> tickets = ticketRepository.findBySesionCajaId(sesionAbierta.getId());
        BigDecimal totalEfectivo = BigDecimal.ZERO;
        
        for (Ticket t : tickets) {
            if (Boolean.TRUE.equals(t.getActivo()) && t.getEstado() == pe.com.salon.salongestionapi.operaciones.entity.EstadoTicket.PAGADO && t.getMetodoPago() == MetodoPago.EFECTIVO) {
                totalEfectivo = totalEfectivo.add(t.getTotal());
            }
        }
        
        List<pe.com.salon.salongestionapi.operaciones.entity.Egreso> egresos = egresoRepository.findBySesionCajaIdOrderByFechaHoraDesc(sesionAbierta.getId());
        BigDecimal totalEgresos = BigDecimal.ZERO;
        for (pe.com.salon.salongestionapi.operaciones.entity.Egreso eg : egresos) {
            totalEgresos = totalEgresos.add(eg.getMonto());
        }
        
        BigDecimal montoEsperadoCalculado = sesionAbierta.getMontoInicial().add(totalEfectivo).subtract(totalEgresos); 
        
        sesionAbierta.setMontoEsperado(montoEsperadoCalculado);
        sesionAbierta.setMontoDeclarado(request.getMontoDeclarado());
        sesionAbierta.setDescuadre(request.getMontoDeclarado().subtract(montoEsperadoCalculado));
        sesionAbierta.setObservaciones(request.getObservaciones());

        SesionCaja cerrada = sesionCajaRepository.save(sesionAbierta);
        return mapToResponse(cerrada);
    }

    public SesionCajaResponse obtenerCajaActual() {
        SesionCaja sesionAbierta = sesionCajaRepository.findByEstadoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna caja abierta actualmente."));
        return mapToResponse(sesionAbierta);
    }

    public pe.com.salon.salongestionapi.operaciones.dto.ResumenCajaResponse obtenerResumenActual() {
        SesionCaja sesionAbierta = sesionCajaRepository.findByEstadoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna caja abierta actualmente."));

        List<Ticket> tickets = ticketRepository.findBySesionCajaId(sesionAbierta.getId());
        BigDecimal totalEfectivo = BigDecimal.ZERO;
        BigDecimal totalTransferencia = BigDecimal.ZERO;

        for (Ticket t : tickets) {
            if (Boolean.TRUE.equals(t.getActivo()) && t.getEstado() == pe.com.salon.salongestionapi.operaciones.entity.EstadoTicket.PAGADO) {
                if (t.getMetodoPago() == MetodoPago.EFECTIVO) {
                    totalEfectivo = totalEfectivo.add(t.getTotal());
                } else {
                    totalTransferencia = totalTransferencia.add(t.getTotal());
                }
            }
        }

        List<pe.com.salon.salongestionapi.operaciones.entity.Egreso> egresos = egresoRepository.findBySesionCajaIdOrderByFechaHoraDesc(sesionAbierta.getId());
        BigDecimal totalEgresos = BigDecimal.ZERO;
        for (pe.com.salon.salongestionapi.operaciones.entity.Egreso eg : egresos) {
            totalEgresos = totalEgresos.add(eg.getMonto());
        }

        pe.com.salon.salongestionapi.operaciones.dto.ResumenCajaResponse resumen = new pe.com.salon.salongestionapi.operaciones.dto.ResumenCajaResponse();
        resumen.setMontoInicial(sesionAbierta.getMontoInicial());
        resumen.setTotalVentasEfectivo(totalEfectivo);
        resumen.setTotalVentasTransferencia(totalTransferencia);
        resumen.setTotalEgresosEfectivo(totalEgresos); 
        resumen.setTotalEsperadoEfectivo(sesionAbierta.getMontoInicial().add(totalEfectivo).subtract(totalEgresos));
        resumen.setCantidadTickets((int) tickets.stream().filter(t -> Boolean.TRUE.equals(t.getActivo()) && t.getEstado() == pe.com.salon.salongestionapi.operaciones.entity.EstadoTicket.PAGADO).count());

        return resumen;
    }

    public List<Ticket> obtenerTicketsCajaActual() {
        SesionCaja sesionAbierta = sesionCajaRepository.findByEstadoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna caja abierta actualmente."));
        return ticketRepository.findBySesionCajaId(sesionAbierta.getId());
    }

    public TotalesPeriodoResponse obtenerTotalesPeriodo() {
        ZoneId zonePeru = ZoneId.of("America/Lima");
        LocalDate hoy = LocalDate.now(zonePeru);

        // Hoy
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime finHoy = hoy.atTime(LocalTime.MAX);
        BigDecimal totalHoy = ticketRepository.sumVentasByFechaEmisionBetween(inicioHoy, finHoy);
        long cantHoy = ticketRepository.countVentasByFechaEmisionBetween(inicioHoy, finHoy);

        // Esta Semana (Lunes a Domingo)
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate domingo = hoy.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDateTime inicioSemana = lunes.atStartOfDay();
        LocalDateTime finSemana = domingo.atTime(LocalTime.MAX);
        BigDecimal totalSemana = ticketRepository.sumVentasByFechaEmisionBetween(inicioSemana, finSemana);
        long cantSemana = ticketRepository.countVentasByFechaEmisionBetween(inicioSemana, finSemana);

        // Este Mes (Día 1 al último día)
        LocalDate primeroMes = hoy.withDayOfMonth(1);
        LocalDate ultimoMes = hoy.with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime inicioMes = primeroMes.atStartOfDay();
        LocalDateTime finMes = ultimoMes.atTime(LocalTime.MAX);
        BigDecimal totalMes = ticketRepository.sumVentasByFechaEmisionBetween(inicioMes, finMes);
        long cantMes = ticketRepository.countVentasByFechaEmisionBetween(inicioMes, finMes);

        return TotalesPeriodoResponse.builder()
                .totalHoy(totalHoy != null ? totalHoy : BigDecimal.ZERO)
                .cantidadVentasHoy(cantHoy)
                .totalSemana(totalSemana != null ? totalSemana : BigDecimal.ZERO)
                .cantidadVentasSemana(cantSemana)
                .totalMes(totalMes != null ? totalMes : BigDecimal.ZERO)
                .cantidadVentasMes(cantMes)
                .build();
    }

    public MovimientosCajaResponse obtenerMovimientosPeriodo(String periodo, String fechaFiltro) {
        ZoneId zonePeru = ZoneId.of("America/Lima");
        LocalDate hoy = LocalDate.now(zonePeru);
        List<Ticket> tickets;
        String periodoEfectivo = periodo != null ? periodo.toUpperCase() : "TURNO";

        if ("TURNO".equals(periodoEfectivo)) {
            // Tickets del turno actualmente abierto
            SesionCaja sesionAbierta = sesionCajaRepository.findByEstadoTrue().orElse(null);
            if (sesionAbierta != null) {
                tickets = ticketRepository.findBySesionCajaId(sesionAbierta.getId()).stream()
                        .filter(t -> Boolean.TRUE.equals(t.getActivo()) && t.getEstado() == EstadoTicket.PAGADO)
                        .sorted((t1, t2) -> t2.getFechaEmision().compareTo(t1.getFechaEmision()))
                        .collect(Collectors.toList());
            } else {
                tickets = Collections.emptyList();
            }
        } else {
            LocalDateTime inicio;
            LocalDateTime fin;

            if ("SEMANA".equals(periodoEfectivo)) {
                LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate domingo = hoy.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                inicio = lunes.atStartOfDay();
                fin = domingo.atTime(LocalTime.MAX);
            } else if ("MES".equals(periodoEfectivo)) {
                LocalDate primeroMes = hoy.withDayOfMonth(1);
                LocalDate ultimoMes = hoy.with(TemporalAdjusters.lastDayOfMonth());
                inicio = primeroMes.atStartOfDay();
                fin = ultimoMes.atTime(LocalTime.MAX);
            } else if ("FECHA".equals(periodoEfectivo) && fechaFiltro != null && !fechaFiltro.isBlank()) {
                LocalDate dia = LocalDate.parse(fechaFiltro.trim());
                inicio = dia.atStartOfDay();
                fin = dia.atTime(LocalTime.MAX);
            } else {
                // Default HOY
                periodoEfectivo = "HOY";
                inicio = hoy.atStartOfDay();
                fin = hoy.atTime(LocalTime.MAX);
            }

            tickets = ticketRepository.findVentasByFechaEmisionBetween(inicio, fin);
        }

        // Totales y desglose
        BigDecimal totalVentas = BigDecimal.ZERO;
        BigDecimal totalEfectivo = BigDecimal.ZERO;
        BigDecimal totalTransferencia = BigDecimal.ZERO;

        for (Ticket t : tickets) {
            BigDecimal m = t.getTotal() != null ? t.getTotal() : BigDecimal.ZERO;
            totalVentas = totalVentas.add(m);
            if (t.getMetodoPago() == MetodoPago.EFECTIVO) {
                totalEfectivo = totalEfectivo.add(m);
            } else {
                totalTransferencia = totalTransferencia.add(m);
            }
        }

        // Agrupación por Trabajadora
        Map<Long, TrabajadoraResumenDTO> mapTrabajadoras = new LinkedHashMap<>();

        for (Ticket t : tickets) {
            for (TicketDetalle d : t.getDetalles()) {
                if (d.getEmpleado() != null) {
                    Long empId = d.getEmpleado().getId();
                    String nombreEmp = (d.getEmpleado().getNombres() + " " + (d.getEmpleado().getApellidos() != null ? d.getEmpleado().getApellidos() : "")).trim();
                    TrabajadoraResumenDTO resumen = mapTrabajadoras.computeIfAbsent(empId, id -> TrabajadoraResumenDTO.builder()
                            .empleadoId(id)
                            .nombreCompleto(nombreEmp)
                            .cantidadServicios(0)
                            .totalGenerado(BigDecimal.ZERO)
                            .serviciosDetalle(new ArrayList<>())
                            .build());

                    int cant = d.getCantidad() != null ? d.getCantidad() : 1;
                    BigDecimal subtotal = d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO;

                    resumen.setCantidadServicios(resumen.getCantidadServicios() + cant);
                    resumen.setTotalGenerado(resumen.getTotalGenerado().add(subtotal));

                    String itemNombre = d.getServicio() != null ? d.getServicio().getNombre() : (d.getProducto() != null ? d.getProducto().getNombre() : "Servicio");
                    TrabajadoraResumenDTO.ServicioConteoDTO conteo = resumen.getServiciosDetalle().stream()
                            .filter(s -> s.getServicioNombre().equalsIgnoreCase(itemNombre))
                            .findFirst()
                            .orElse(null);

                    if (conteo != null) {
                        conteo.setCantidad(conteo.getCantidad() + cant);
                        conteo.setSubtotal(conteo.getSubtotal().add(subtotal));
                    } else {
                        resumen.getServiciosDetalle().add(TrabajadoraResumenDTO.ServicioConteoDTO.builder()
                                .servicioNombre(itemNombre)
                                .cantidad(cant)
                                .subtotal(subtotal)
                                .build());
                    }
                }
            }
        }

        List<TrabajadoraResumenDTO> ranking = new ArrayList<>(mapTrabajadoras.values());
        ranking.sort((r1, r2) -> r2.getTotalGenerado().compareTo(r1.getTotalGenerado()));

        List<TicketResponse> ticketsRes = tickets.stream()
                .map(ticketService::mapToResponse)
                .collect(Collectors.toList());

        return MovimientosCajaResponse.builder()
                .periodo(periodoEfectivo)
                .fechaFiltro(fechaFiltro)
                .totalVentas(totalVentas)
                .totalEfectivo(totalEfectivo)
                .totalTransferencia(totalTransferencia)
                .cantidadTickets(tickets.size())
                .tickets(ticketsRes)
                .rankingTrabajadoras(ranking)
                .build();
    }

    private SesionCajaResponse mapToResponse(SesionCaja sesionCaja) {
        SesionCajaResponse response = new SesionCajaResponse();
        response.setId(sesionCaja.getId());
        response.setFechaApertura(sesionCaja.getFechaApertura());
        response.setFechaCierre(sesionCaja.getFechaCierre());
        response.setMontoInicial(sesionCaja.getMontoInicial());
        response.setMontoEsperado(sesionCaja.getMontoEsperado());
        response.setMontoDeclarado(sesionCaja.getMontoDeclarado());
        response.setDescuadre(sesionCaja.getDescuadre());
        response.setObservaciones(sesionCaja.getObservaciones());
        response.setEstado(sesionCaja.getEstado());
        
        if (sesionCaja.getUsuarioApertura() != null) {
            response.setUsuarioAperturaId(sesionCaja.getUsuarioApertura().getId());
            response.setUsuarioAperturaNombre(sesionCaja.getUsuarioApertura().getUsername());
        }
        
        return response;
    }
}
