package pe.com.salon.salongestionapi.rrhh.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.rrhh.dto.AsistenciaRequest;
import pe.com.salon.salongestionapi.rrhh.dto.AsistenciaResponse;
import pe.com.salon.salongestionapi.rrhh.service.AsistenciaService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rrhh/asistencias")
@RequiredArgsConstructor
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    @PostMapping
    public ResponseEntity<AsistenciaResponse> registrarEntrada(@Valid @RequestBody AsistenciaRequest request) {
        AsistenciaResponse registro = asistenciaService.registrar(request);
        return new ResponseEntity<>(registro, HttpStatus.CREATED);
    }

    @PatchMapping("/{empleadoId}/salida")
    public ResponseEntity<AsistenciaResponse> registrarSalida(
            @PathVariable Long empleadoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(asistenciaService.registrarSalida(empleadoId, fecha));
    }

    @GetMapping("/empleado/{empleadoId}")
    public ResponseEntity<List<AsistenciaResponse>> listarPorEmpleado(@PathVariable Long empleadoId) {
        return ResponseEntity.ok(asistenciaService.listarPorEmpleado(empleadoId));
    }

    @GetMapping
    public ResponseEntity<List<AsistenciaResponse>> listarPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(asistenciaService.listarPorRangoDeFechas(inicio, fin));
    }
}
