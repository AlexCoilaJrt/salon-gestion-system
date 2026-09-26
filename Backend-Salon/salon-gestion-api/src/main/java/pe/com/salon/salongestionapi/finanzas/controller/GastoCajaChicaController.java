package pe.com.salon.salongestionapi.finanzas.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.finanzas.dto.GastoRequest;
import pe.com.salon.salongestionapi.finanzas.dto.GastoResponse;
import pe.com.salon.salongestionapi.finanzas.service.GastoCajaChicaService;

import java.util.List;

@RestController
@RequestMapping("/api/finanzas/gastos")
@RequiredArgsConstructor
public class GastoCajaChicaController {

    private final GastoCajaChicaService gastoService;

    @GetMapping
    public ResponseEntity<List<GastoResponse>> listarGastos() {
        return ResponseEntity.ok(gastoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GastoResponse> obtenerGasto(@PathVariable Long id) {
        return ResponseEntity.ok(gastoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<GastoResponse> registrarGasto(@Valid @RequestBody GastoRequest request) {
        GastoResponse nuevoGasto = gastoService.registrarGasto(request);
        return new ResponseEntity<>(nuevoGasto, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GastoResponse> actualizarGasto(
            @PathVariable Long id,
            @Valid @RequestBody GastoRequest request) {
        return ResponseEntity.ok(gastoService.actualizarGasto(id, request));
    }

    @DeleteMapping("/{id}/anular")
    public ResponseEntity<Void> anularGasto(@PathVariable Long id) {
        gastoService.anularGasto(id);
        return ResponseEntity.noContent().build();
    }
}
