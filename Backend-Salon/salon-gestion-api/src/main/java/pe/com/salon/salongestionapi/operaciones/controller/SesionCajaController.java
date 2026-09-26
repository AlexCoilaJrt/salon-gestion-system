package pe.com.salon.salongestionapi.operaciones.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.operaciones.dto.SesionCajaRequest;
import pe.com.salon.salongestionapi.operaciones.dto.SesionCajaResponse;
import pe.com.salon.salongestionapi.operaciones.service.SesionCajaService;

@RestController
@RequestMapping("/api/operaciones/caja")
@RequiredArgsConstructor
public class SesionCajaController {

    private final SesionCajaService sesionCajaService;

    @PostMapping("/abrir")
    public ResponseEntity<SesionCajaResponse> abrirCaja(@Valid @RequestBody SesionCajaRequest request) {
        SesionCajaResponse cajaAbierta = sesionCajaService.abrirCaja(request);
        return new ResponseEntity<>(cajaAbierta, HttpStatus.CREATED);
    }

    @PostMapping("/cerrar")
    public ResponseEntity<SesionCajaResponse> cerrarCaja() {
        return ResponseEntity.ok(sesionCajaService.cerrarCaja());
    }

    @GetMapping("/actual")
    public ResponseEntity<SesionCajaResponse> obtenerCajaActual() {
        return ResponseEntity.ok(sesionCajaService.obtenerCajaActual());
    }
}
