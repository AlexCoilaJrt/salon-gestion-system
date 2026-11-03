package pe.com.salon.salongestionapi.rrhh.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.rrhh.dto.AdelantoRequest;
import pe.com.salon.salongestionapi.rrhh.dto.AdelantoResponse;
import pe.com.salon.salongestionapi.rrhh.service.AdelantoService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rrhh/adelantos")
@RequiredArgsConstructor
public class AdelantoController {

    private final AdelantoService adelantoService;

    @PostMapping
    public ResponseEntity<AdelantoResponse> registrarAdelanto(@RequestBody AdelantoRequest request) {
        AdelantoResponse response = adelantoService.registrarAdelanto(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AdelantoResponse>> obtenerAdelantos(
            @RequestParam(required = false) Long empleadoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        List<AdelantoResponse> responses = adelantoService.obtenerAdelantosPorEmpleadoYPeriodo(empleadoId, fechaInicio, fechaFin);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}/anular")
    public ResponseEntity<AdelantoResponse> anularAdelanto(@PathVariable Long id) {
        AdelantoResponse response = adelantoService.anularAdelanto(id);
        return ResponseEntity.ok(response);
    }
}

