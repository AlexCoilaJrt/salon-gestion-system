package pe.com.salon.salongestionapi.rrhh.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.rrhh.dto.LiquidacionRequest;
import pe.com.salon.salongestionapi.rrhh.dto.LiquidacionResponse;
import pe.com.salon.salongestionapi.rrhh.service.LiquidacionService;

import java.util.List;
import java.util.Map;

@RestController("rrhhLiquidacionController")
@RequestMapping("/api/rrhh/liquidaciones")
@RequiredArgsConstructor
public class LiquidacionController {

    private final LiquidacionService liquidacionService;

    @PostMapping
    public ResponseEntity<LiquidacionResponse> crearLiquidacion(@Valid @RequestBody LiquidacionRequest request) {
        return new ResponseEntity<>(liquidacionService.crearLiquidacion(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<LiquidacionResponse>> obtenerTodas() {
        return ResponseEntity.ok(liquidacionService.obtenerTodas());
    }

    @GetMapping("/empleado/{empleadoId}")
    public ResponseEntity<List<LiquidacionResponse>> obtenerPorEmpleado(@PathVariable Long empleadoId) {
        return ResponseEntity.ok(liquidacionService.obtenerPorEmpleado(empleadoId));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<LiquidacionResponse> actualizarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String nuevoEstado = body.get("estado");
        return ResponseEntity.ok(liquidacionService.actualizarEstado(id, nuevoEstado));
    }
}
