package pe.com.salon.salongestionapi.operaciones.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.operaciones.dto.SesionCajaRequest;
import pe.com.salon.salongestionapi.operaciones.dto.SesionCajaResponse;
import pe.com.salon.salongestionapi.operaciones.entity.SesionCaja;
import pe.com.salon.salongestionapi.operaciones.repository.SesionCajaRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SesionCajaService {

    private final SesionCajaRepository sesionCajaRepository;

    public SesionCajaResponse abrirCaja(SesionCajaRequest request) {
        // Verificar si ya hay una caja abierta
        if (sesionCajaRepository.findByEstadoTrue().isPresent()) {
            throw new RuntimeException("Ya existe una caja abierta actualmente. Debe cerrarla primero.");
        }

        SesionCaja sesionCaja = new SesionCaja();
        sesionCaja.setFechaApertura(LocalDateTime.now());
        sesionCaja.setMontoInicial(request.getMontoInicial());
        sesionCaja.setEstado(true);

        SesionCaja guardada = sesionCajaRepository.save(sesionCaja);
        return mapToResponse(guardada);
    }

    public SesionCajaResponse cerrarCaja() {
        SesionCaja sesionAbierta = sesionCajaRepository.findByEstadoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna caja abierta actualmente."));

        sesionAbierta.setFechaCierre(LocalDateTime.now());
        sesionAbierta.setEstado(false);
        // El montoFinal real se calcularía sumando todos los tickets + montoInicial, 
        // pero por ahora solo la cerraremos lógicamente.

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
        response.setMontoFinal(sesionCaja.getMontoFinal());
        response.setEstado(sesionCaja.getEstado());
        return response;
    }
}
