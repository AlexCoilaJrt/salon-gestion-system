package pe.com.salon.salongestionapi.operaciones.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.operaciones.dto.CitaRequest;
import pe.com.salon.salongestionapi.operaciones.dto.CitaResponse;
import pe.com.salon.salongestionapi.operaciones.service.CitaService;

import java.util.List;

@RestController
@RequestMapping("/api/operaciones/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;

    @GetMapping
    public ResponseEntity<List<CitaResponse>> listarCitas() {
        return ResponseEntity.ok(citaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CitaResponse> obtenerCita(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<CitaResponse> crearCita(@Valid @RequestBody CitaRequest request) {
        CitaResponse nuevaCita = citaService.crearCita(request);
        return new ResponseEntity<>(nuevaCita, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CitaResponse> actualizarCita(
            @PathVariable Long id,
            @Valid @RequestBody CitaRequest request) {
        return ResponseEntity.ok(citaService.actualizarCita(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCita(@PathVariable Long id) {
        citaService.eliminarCita(id);
        return ResponseEntity.noContent().build();
    }
}
