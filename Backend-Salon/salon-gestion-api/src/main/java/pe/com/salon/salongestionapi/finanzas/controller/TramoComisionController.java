package pe.com.salon.salongestionapi.finanzas.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.finanzas.dto.TramoComisionRequest;
import pe.com.salon.salongestionapi.finanzas.dto.TramoComisionResponse;
import pe.com.salon.salongestionapi.finanzas.service.TramoComisionService;

import java.util.List;

@RestController
@RequestMapping("/api/finanzas/tramos-comision")
@RequiredArgsConstructor
public class TramoComisionController {

    private final TramoComisionService tramoComisionService;

    @GetMapping
    public ResponseEntity<List<TramoComisionResponse>> listar() {
        return ResponseEntity.ok(tramoComisionService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<TramoComisionResponse> crear(@Valid @RequestBody TramoComisionRequest request) {
        return new ResponseEntity<>(tramoComisionService.crear(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TramoComisionResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody TramoComisionRequest request) {
        return ResponseEntity.ok(tramoComisionService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        tramoComisionService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
