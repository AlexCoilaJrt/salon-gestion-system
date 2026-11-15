package pe.com.salon.salongestionapi.operaciones.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.operaciones.dto.impresion.ImpresoraConfigDTO;
import pe.com.salon.salongestionapi.operaciones.dto.impresion.ImpresoraTestResultDTO;
import pe.com.salon.salongestionapi.operaciones.service.impresion.ImpresoraTermicaService;

@RestController
@RequestMapping("/api/operaciones/impresora")
@RequiredArgsConstructor
public class ImpresoraController {

    private final ImpresoraTermicaService impresoraTermicaService;

    @GetMapping("/config")
    public ResponseEntity<ImpresoraConfigDTO> getConfiguracion() {
        return ResponseEntity.ok(impresoraTermicaService.getConfiguracion());
    }

    @PutMapping("/config")
    public ResponseEntity<ImpresoraConfigDTO> guardarConfiguracion(@RequestBody ImpresoraConfigDTO dto) {
        return ResponseEntity.ok(impresoraTermicaService.guardarConfiguracion(dto));
    }

    @PostMapping("/test-conexion")
    public ResponseEntity<ImpresoraTestResultDTO> probarConexion(@RequestBody(required = false) ImpresoraConfigDTO dto) {
        String ip = dto != null ? dto.getIp() : null;
        Integer puerto = dto != null ? dto.getPuerto() : null;
        return ResponseEntity.ok(impresoraTermicaService.probarConexion(ip, puerto));
    }

    @PostMapping("/test-ticket")
    public ResponseEntity<ImpresoraTestResultDTO> imprimirTicketPrueba(@RequestBody(required = false) ImpresoraConfigDTO dto) {
        String ip = dto != null ? dto.getIp() : null;
        Integer puerto = dto != null ? dto.getPuerto() : null;
        return ResponseEntity.ok(impresoraTermicaService.imprimirTicketPrueba(ip, puerto));
    }

    @PostMapping("/tickets/{ticketId}/imprimir")
    public ResponseEntity<Void> imprimirTicketVenta(@PathVariable Long ticketId) {
        impresoraTermicaService.imprimirTicketVenta(ticketId);
        return ResponseEntity.ok().build();
    }
}

