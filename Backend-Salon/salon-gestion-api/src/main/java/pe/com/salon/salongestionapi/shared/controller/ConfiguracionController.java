package pe.com.salon.salongestionapi.shared.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.shared.entity.ConfiguracionSistema;
import pe.com.salon.salongestionapi.shared.service.ConfiguracionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    @GetMapping
    public ResponseEntity<List<ConfiguracionSistema>> listarTodas() {
        return ResponseEntity.ok(configuracionService.listarTodas());
    }

    @PutMapping("/{clave}")
    public ResponseEntity<ConfiguracionSistema> actualizar(
            @PathVariable String clave,
            @RequestBody Map<String, String> body) {
        String nuevoValor = body.get("valor");
        if (nuevoValor == null || nuevoValor.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(configuracionService.actualizar(clave, nuevoValor));
    }
}
