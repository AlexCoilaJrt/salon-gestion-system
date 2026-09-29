package pe.com.salon.salongestionapi.rrhh.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.rrhh.dto.ComisionRequest;
import pe.com.salon.salongestionapi.rrhh.dto.ComisionResponse;
import pe.com.salon.salongestionapi.rrhh.service.ComisionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comisiones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ComisionController {

    private final ComisionService comisionService;

    @GetMapping
    public ResponseEntity<List<ComisionResponse>> findAll() {
        return ResponseEntity.ok(comisionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComisionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(comisionService.findById(id));
    }

    @GetMapping("/empleado/{empleadoId}")
    public ResponseEntity<List<ComisionResponse>> findByEmpleadoId(@PathVariable Long empleadoId) {
        return ResponseEntity.ok(comisionService.findByEmpleadoId(empleadoId));
    }

    @PostMapping
    public ResponseEntity<ComisionResponse> create(@Valid @RequestBody ComisionRequest request) {
        return new ResponseEntity<>(comisionService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ComisionResponse> update(@PathVariable Long id, @Valid @RequestBody ComisionRequest request) {
        return ResponseEntity.ok(comisionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        comisionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
