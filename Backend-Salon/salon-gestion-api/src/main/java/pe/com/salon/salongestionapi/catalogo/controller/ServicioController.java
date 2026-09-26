package pe.com.salon.salongestionapi.catalogo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.catalogo.dto.ServicioRequest;
import pe.com.salon.salongestionapi.catalogo.dto.ServicioResponse;
import pe.com.salon.salongestionapi.catalogo.service.ServicioService;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioService servicioService;

    @GetMapping
    public ResponseEntity<List<ServicioResponse>> listarServicios() {
        return ResponseEntity.ok(servicioService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioResponse> obtenerServicio(@PathVariable Long id) {
        return ResponseEntity.ok(servicioService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<ServicioResponse> crearServicio(@Valid @RequestBody ServicioRequest request) {
        ServicioResponse nuevoServicio = servicioService.crearServicio(request);
        return new ResponseEntity<>(nuevoServicio, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicioResponse> actualizarServicio(
            @PathVariable Long id,
            @Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.ok(servicioService.actualizarServicio(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarServicio(@PathVariable Long id) {
        servicioService.eliminarServicio(id);
        return ResponseEntity.noContent().build();
    }
}
