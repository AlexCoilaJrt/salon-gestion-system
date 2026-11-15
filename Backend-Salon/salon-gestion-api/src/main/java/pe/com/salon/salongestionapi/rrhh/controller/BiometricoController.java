package pe.com.salon.salongestionapi.rrhh.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.rrhh.dto.biometrico.BiometricoConfigDTO;
import pe.com.salon.salongestionapi.rrhh.dto.biometrico.BiometricoSincronizacionDTO;
import pe.com.salon.salongestionapi.rrhh.dto.biometrico.BiometricoTestResultDTO;
import pe.com.salon.salongestionapi.rrhh.service.biometrico.BiometricoService;

@RestController
@RequestMapping("/api/rrhh/biometrico")
@RequiredArgsConstructor
public class BiometricoController {

    private final BiometricoService biometricoService;

    @GetMapping("/config")
    public ResponseEntity<BiometricoConfigDTO> getConfiguracion() {
        return ResponseEntity.ok(biometricoService.getConfiguracion());
    }

    @PutMapping("/config")
    public ResponseEntity<BiometricoConfigDTO> guardarConfiguracion(@RequestBody BiometricoConfigDTO dto) {
        return ResponseEntity.ok(biometricoService.guardarConfiguracion(dto));
    }

    @PostMapping("/test-conexion")
    public ResponseEntity<BiometricoTestResultDTO> probarConexion(@RequestBody(required = false) BiometricoConfigDTO dto) {
        String ip = dto != null ? dto.getIp() : null;
        Integer puerto = dto != null ? dto.getPuerto() : null;
        return ResponseEntity.ok(biometricoService.probarConexion(ip, puerto));
    }

    @PostMapping("/sincronizar")
    public ResponseEntity<BiometricoSincronizacionDTO> sincronizarMarcaciones() {
        return ResponseEntity.ok(biometricoService.sincronizarMarcaciones());
    }
}
