package pe.com.salon.salongestionapi.finanzas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.finanzas.dto.GastoRequest;
import pe.com.salon.salongestionapi.finanzas.dto.GastoResponse;
import pe.com.salon.salongestionapi.finanzas.entity.GastoCajaChica;
import pe.com.salon.salongestionapi.finanzas.repository.GastoCajaChicaRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GastoCajaChicaService {

    private final GastoCajaChicaRepository gastoCajaChicaRepository;

    public List<GastoResponse> listarTodos() {
        return gastoCajaChicaRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public GastoResponse obtenerPorId(Long id) {
        GastoCajaChica gasto = gastoCajaChicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado con id: " + id));
        return mapToResponse(gasto);
    }

    public GastoResponse registrarGasto(GastoRequest request) {
        GastoCajaChica gasto = new GastoCajaChica();
        gasto.setMonto(request.getMonto());
        gasto.setDescripcion(request.getDescripcion());
        gasto.setFechaGasto(LocalDateTime.now());
        gasto.setEstado(true);

        GastoCajaChica guardado = gastoCajaChicaRepository.save(gasto);
        return mapToResponse(guardado);
    }

    public GastoResponse actualizarGasto(Long id, GastoRequest request) {
        GastoCajaChica gasto = gastoCajaChicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado con id: " + id));

        gasto.setMonto(request.getMonto());
        gasto.setDescripcion(request.getDescripcion());

        GastoCajaChica actualizado = gastoCajaChicaRepository.save(gasto);
        return mapToResponse(actualizado);
    }

    public void anularGasto(Long id) {
        GastoCajaChica gasto = gastoCajaChicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado con id: " + id));
        
        gasto.setEstado(false);
        gastoCajaChicaRepository.save(gasto);
    }

    private GastoResponse mapToResponse(GastoCajaChica gasto) {
        GastoResponse res = new GastoResponse();
        res.setId(gasto.getId());
        res.setMonto(gasto.getMonto());
        res.setDescripcion(gasto.getDescripcion());
        res.setFechaGasto(gasto.getFechaGasto());
        res.setEstado(gasto.getEstado());
        return res;
    }
}
