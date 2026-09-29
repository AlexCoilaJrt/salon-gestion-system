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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SesionCajaService {

    private final SesionCajaRepository sesionCajaRepository;
    private final UsuarioRepository usuarioRepository;

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
        
        // TODO: sumar tickets y egresos
        BigDecimal montoEsperadoCalculado = sesionAbierta.getMontoInicial(); 
        
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
