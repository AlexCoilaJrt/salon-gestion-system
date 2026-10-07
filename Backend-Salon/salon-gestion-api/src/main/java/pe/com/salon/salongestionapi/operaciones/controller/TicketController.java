package pe.com.salon.salongestionapi.operaciones.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.operaciones.dto.TicketRequest;
import pe.com.salon.salongestionapi.operaciones.dto.TicketResponse;
import pe.com.salon.salongestionapi.operaciones.service.TicketService;

@RestController
@RequestMapping("/api/operaciones/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/generar-orden")
    public ResponseEntity<TicketResponse> generarOrden(@Valid @RequestBody TicketRequest request) {
        TicketResponse ordenGenerada = ticketService.generarOrden(request);
        return new ResponseEntity<>(ordenGenerada, HttpStatus.CREATED);
    }

    @PostMapping
    public ResponseEntity<TicketResponse> emitirTicket(@Valid @RequestBody TicketRequest request) {
        TicketResponse nuevoTicket = ticketService.emitirTicket(request);
        return new ResponseEntity<>(nuevoTicket, HttpStatus.CREATED);
    }

    @GetMapping("/caja-actual")
    public ResponseEntity<java.util.List<TicketResponse>> obtenerTicketsCajaActual() {
        return ResponseEntity.ok(ticketService.obtenerTicketsCajaActual());
    }

    @GetMapping
    public ResponseEntity<java.util.List<TicketResponse>> obtenerTodosTickets() {
        return ResponseEntity.ok(ticketService.obtenerTodosTickets());
    }

    @PutMapping("/{id}/anular")
    public ResponseEntity<TicketResponse> anularTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.anularTicket(id));
    }

    @GetMapping("/pendientes")
    public ResponseEntity<java.util.List<TicketResponse>> obtenerTicketsPendientes() {
        return ResponseEntity.ok(ticketService.obtenerTicketsPendientes());
    }

    @PutMapping("/{id}/pagar")
    public ResponseEntity<TicketResponse> pagarTicket(@PathVariable Long id, @Valid @RequestBody TicketRequest request) {
        return ResponseEntity.ok(ticketService.pagarTicket(id, request));
    }
}
