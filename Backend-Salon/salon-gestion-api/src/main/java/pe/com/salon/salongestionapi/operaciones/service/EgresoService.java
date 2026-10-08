package pe.com.salon.salongestionapi.operaciones.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.operaciones.dto.EgresoDTO;
import pe.com.salon.salongestionapi.operaciones.dto.EgresoRequest;
import pe.com.salon.salongestionapi.operaciones.entity.Egreso;
import pe.com.salon.salongestionapi.operaciones.entity.SesionCaja;
import pe.com.salon.salongestionapi.operaciones.repository.EgresoRepository;
import pe.com.salon.salongestionapi.operaciones.repository.SesionCajaRepository;
import pe.com.salon.salongestionapi.security.entity.Usuario;
import pe.com.salon.salongestionapi.security.repository.UsuarioRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
public class EgresoService {

    private final EgresoRepository egresoRepository;
    private final SesionCajaRepository sesionCajaRepository;
    private final UsuarioRepository usuarioRepository;

    public EgresoDTO registrarEgreso(EgresoRequest request) {
        SesionCaja sesionAbierta = sesionCajaRepository.findByEstadoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna caja abierta."));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con username: " + username));

        Egreso egreso = new Egreso();
        egreso.setMonto(request.getMonto());
        egreso.setCategoria(request.getCategoria());
        egreso.setMotivo(request.getMotivo());
        egreso.setFechaHora(LocalDateTime.now());
        egreso.setSesionCaja(sesionAbierta);
        egreso.setUsuario(usuario);

        Egreso guardado = egresoRepository.save(egreso);
        return mapToDTO(guardado);
    }

    public List<EgresoDTO> obtenerEgresosCajaActual() {
        SesionCaja sesionAbierta = sesionCajaRepository.findByEstadoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna caja abierta."));

        return egresoRepository.findBySesionCajaIdOrderByFechaHoraDesc(sesionAbierta.getId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private EgresoDTO mapToDTO(Egreso egreso) {
        EgresoDTO dto = new EgresoDTO();
        dto.setId(egreso.getId());
        dto.setMonto(egreso.getMonto());
        dto.setCategoria(egreso.getCategoria());
        dto.setMotivo(egreso.getMotivo());
        dto.setFechaHora(egreso.getFechaHora());
        dto.setUsuarioNombre(egreso.getUsuario().getUsername());
        return dto;
    }
}
