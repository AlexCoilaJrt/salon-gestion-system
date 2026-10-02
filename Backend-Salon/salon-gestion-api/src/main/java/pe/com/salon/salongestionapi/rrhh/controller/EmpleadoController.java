package pe.com.salon.salongestionapi.rrhh.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import pe.com.salon.salongestionapi.rrhh.dto.EmpleadoRequest;
import pe.com.salon.salongestionapi.rrhh.dto.EmpleadoResponse;
import pe.com.salon.salongestionapi.rrhh.service.EmpleadoService;
import pe.com.salon.salongestionapi.shared.PageResponse;

import java.util.List;

@RestController
@RequestMapping("/api/rrhh/empleados")
@RequiredArgsConstructor
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    @GetMapping
    public ResponseEntity<List<EmpleadoResponse>> listarEmpleados() {
        return ResponseEntity.ok(empleadoService.listarTodos());
    }

    @GetMapping("/paginado")
    public ResponseEntity<PageResponse<EmpleadoResponse>> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(empleadoService.listarPaginado(page, size, search));
    }

    @PostMapping
    public ResponseEntity<EmpleadoResponse> crearEmpleado(@Valid @RequestBody EmpleadoRequest request) {
        EmpleadoResponse nuevoEmpleado = empleadoService.crearEmpleado(request);
        return new ResponseEntity<>(nuevoEmpleado, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpleadoResponse> obtenerEmpleado(@PathVariable Long id) {
        return ResponseEntity.ok(empleadoService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmpleadoResponse> actualizarEmpleado(
            @PathVariable Long id,
            @Valid @RequestBody EmpleadoRequest request) {
        return ResponseEntity.ok(empleadoService.actualizarEmpleado(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEmpleado(@PathVariable Long id) {
        empleadoService.eliminarEmpleado(id);
        return ResponseEntity.noContent().build();
    }
}
