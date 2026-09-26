package pe.com.salon.salongestionapi.rrhh.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.rrhh.dto.EspecialidadRequest;
import pe.com.salon.salongestionapi.rrhh.dto.EspecialidadResponse;
import pe.com.salon.salongestionapi.rrhh.service.EspecialidadService;

import java.util.List;

@RestController
@RequestMapping("/api/rrhh/especialidades")
@RequiredArgsConstructor
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @GetMapping
    public ResponseEntity<List<EspecialidadResponse>> listarEspecialidades() {
        return ResponseEntity.ok(especialidadService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EspecialidadResponse> obtenerEspecialidad(@PathVariable Long id) {
        return ResponseEntity.ok(especialidadService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<EspecialidadResponse> crearEspecialidad(@Valid @RequestBody EspecialidadRequest request) {
        EspecialidadResponse nuevaEspecialidad = especialidadService.crearEspecialidad(request);
        return new ResponseEntity<>(nuevaEspecialidad, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EspecialidadResponse> actualizarEspecialidad(
            @PathVariable Long id,
            @Valid @RequestBody EspecialidadRequest request) {
        return ResponseEntity.ok(especialidadService.actualizarEspecialidad(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEspecialidad(@PathVariable Long id) {
        especialidadService.eliminarEspecialidad(id);
        return ResponseEntity.noContent().build();
    }
}
