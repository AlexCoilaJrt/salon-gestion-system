package pe.com.salon.salongestionapi.operaciones.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.operaciones.dto.EgresoDTO;
import pe.com.salon.salongestionapi.operaciones.dto.EgresoRequest;
import pe.com.salon.salongestionapi.operaciones.service.EgresoService;

import java.util.List;

@RestController
@RequestMapping("/api/operaciones/egresos")
@RequiredArgsConstructor
public class EgresoController {

    private final EgresoService egresoService;

    @PostMapping
    public ResponseEntity<EgresoDTO> registrarEgreso(@RequestBody EgresoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(egresoService.registrarEgreso(request));
    }

    @GetMapping("/caja-actual")
    public ResponseEntity<List<EgresoDTO>> obtenerEgresosCajaActual() {
        return ResponseEntity.ok(egresoService.obtenerEgresosCajaActual());
    }
}
