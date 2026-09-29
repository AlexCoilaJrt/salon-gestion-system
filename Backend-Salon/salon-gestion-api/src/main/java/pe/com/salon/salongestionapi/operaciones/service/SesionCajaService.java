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
import pe.com.salon.salongestionapi.operaciones.entity.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SesionCajaService {

    private final SesionCajaRepository sesionCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TicketRepository ticketRepository;
    private final pe.com.salon.salongestionapi.operaciones.repository.EgresoRepository egresoRepository;

    public SesionCajaResponse abrirCaja(SesionCajaRequest request) {
        if (sesionCajaRepository.findByEstadoTrue().isPresent()) {
            throw new RuntimeException("Ya existe una caja abierta actualmente. Debe cerrarla primero.");
        }

        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + request.getUsuarioId()));

        SesionCaja sesionCaja = new SesionCaja();
        sesionCaja.setUsuarioApertura(usuario);
        sesionCaja.setFechaApertura(LocalDateTime.now());
        sesionCaja.setMontoInicial(request.getMontoInicial());
        sesionCaja.setEstado(true);

        SesionCaja guardada = sesionCajaRepository.save(sesionCaja);
        return mapToResponse(guardada);
    }

    public SesionCajaResponse cerrarCaja(CierreCajaRequest request) {
        SesionCaja sesionAbierta = sesionCajaRepository.findByEstadoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna caja abierta actualmente."));

        sesionAbierta.setFechaCierre(LocalDateTime.now());
        sesionAbierta.setEstado(false);
        
        List<Ticket> tickets = ticketRepository.findBySesionCajaId(sesionAbierta.getId());
        BigDecimal totalEfectivo = BigDecimal.ZERO;
        
        for (Ticket t : tickets) {
            if (Boolean.TRUE.equals(t.getActivo()) && t.getMetodoPago() == MetodoPago.EFECTIVO) {
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
            if (Boolean.TRUE.equals(t.getActivo())) {
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
        resumen.setCantidadTickets((int) tickets.stream().filter(t -> Boolean.TRUE.equals(t.getActivo())).count());

        return resumen;
    }

    public List<Ticket> obtenerTicketsCajaActual() {
        SesionCaja sesionAbierta = sesionCajaRepository.findByEstadoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna caja abierta actualmente."));
        return ticketRepository.findBySesionCajaId(sesionAbierta.getId());
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
