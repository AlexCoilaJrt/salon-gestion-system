package pe.com.salon.salongestionapi.rrhh.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.rrhh.dto.IncentivoRequest;
import pe.com.salon.salongestionapi.rrhh.dto.IncentivoResponse;
import pe.com.salon.salongestionapi.rrhh.service.IncentivoService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incentivos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IncentivoController {

    private final IncentivoService incentivoService;

    @GetMapping
    public ResponseEntity<List<IncentivoResponse>> findAll() {
        return ResponseEntity.ok(incentivoService.findAll());
    }

    @GetMapping("/activos")
    public ResponseEntity<List<IncentivoResponse>> getActiveIncentivos() {
        return ResponseEntity.ok(incentivoService.getActiveIncentivos());
    }

    @GetMapping("/monitor")
    public ResponseEntity<List<pe.com.salon.salongestionapi.rrhh.dto.MonitorComisionResponse>> getMonitorComisiones(
            @RequestParam(value = "fechaInicio", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaFin) {
        return ResponseEntity.ok(incentivoService.getMonitorComisiones(fechaInicio, fechaFin));
    }

    @GetMapping("/liquidaciones")
    public ResponseEntity<List<pe.com.salon.salongestionapi.rrhh.dto.LiquidacionResponse>> getLiquidaciones(
            @RequestParam("fechaInicio") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaInicio,
            @RequestParam("fechaFin") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaFin) {
        return ResponseEntity.ok(incentivoService.getLiquidaciones(fechaInicio, fechaFin));
    }

    // Desglose de servicios/productos que el especialista realizó en el periodo, con su comisión
    @GetMapping("/liquidaciones/detalle")
    public ResponseEntity<List<pe.com.salon.salongestionapi.rrhh.dto.DetalleLiquidacionResponse>> getDetalleLiquidacion(
            @RequestParam("empleadoId") Long empleadoId,
            @RequestParam("fechaInicio") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaInicio,
            @RequestParam("fechaFin") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaFin) {
        return ResponseEntity.ok(incentivoService.getDetalleLiquidacion(empleadoId, fechaInicio, fechaFin));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncentivoResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(incentivoService.findById(id));
    }

    @PostMapping
    public ResponseEntity<IncentivoResponse> create(@Valid @RequestBody IncentivoRequest request) {
        return new ResponseEntity<>(incentivoService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncentivoResponse> update(@PathVariable Long id, @Valid @RequestBody IncentivoRequest request) {
        return ResponseEntity.ok(incentivoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        incentivoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
