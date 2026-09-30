package pe.com.salon.salongestionapi.fidelizacion.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.fidelizacion.dto.CartillaDTO;
import pe.com.salon.salongestionapi.fidelizacion.dto.ClienteCartillaDTO;
import pe.com.salon.salongestionapi.fidelizacion.service.FidelizacionService;

import java.util.List;

@RestController
@RequestMapping("/api/fidelizacion")
@RequiredArgsConstructor
public class FidelizacionController {

    private final FidelizacionService fidelizacionService;

    // --- Configuración de Cartillas (Admin) ---

    @PostMapping("/cartillas")
    public ResponseEntity<CartillaDTO> crearCartilla(@RequestBody CartillaDTO request) {
        return ResponseEntity.ok(fidelizacionService.crearCartilla(request));
    }

    @GetMapping("/cartillas")
    public ResponseEntity<List<CartillaDTO>> listarCartillas() {
        return ResponseEntity.ok(fidelizacionService.listarCartillas());
    }

    // --- Gestión de Clientes ---

    @GetMapping("/clientes/{clienteId}/cartillas")
    public ResponseEntity<List<ClienteCartillaDTO>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(fidelizacionService.listarCartillasPorCliente(clienteId));
    }

    @GetMapping("/clientes/{clienteId}/premios")
    public ResponseEntity<List<ClienteCartillaDTO>> listarPremiosDisponibles(@PathVariable Long clienteId) {
        return ResponseEntity.ok(fidelizacionService.listarPremiosDisponibles(clienteId));
    }

    @PostMapping("/clientes/{clienteId}/canjear/{cartillaId}")
    public ResponseEntity<Void> canjearPremio(@PathVariable Long clienteId, @PathVariable Long cartillaId) {
        fidelizacionService.canjearPremio(clienteId, cartillaId);
        return ResponseEntity.ok().build();
    }
}
