package pe.com.salon.salongestionapi.empresa.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.salon.salongestionapi.empresa.dto.EmpresaResponse;
import pe.com.salon.salongestionapi.empresa.service.EmpresaService;
import pe.com.salon.salongestionapi.shared.ApiResponse;

@RestController
@RequestMapping("/api/v1/empresa")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;

    @GetMapping("/activa")
    public ResponseEntity<ApiResponse<EmpresaResponse>> obtenerEmpresaActiva() {
        EmpresaResponse response = empresaService.obtenerEmpresaActiva();
        return ResponseEntity.ok(ApiResponse.success(response, "Datos de la empresa obtenidos exitosamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmpresaResponse>> actualizarEmpresa(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody pe.com.salon.salongestionapi.empresa.dto.EmpresaRequest request) {
        
        EmpresaResponse response = empresaService.actualizarEmpresa(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Empresa actualizada correctamente"));
    }
}
