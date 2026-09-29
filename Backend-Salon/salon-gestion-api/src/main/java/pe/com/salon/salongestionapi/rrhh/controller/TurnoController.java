package pe.com.salon.salongestionapi.rrhh.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.rrhh.entity.Turno;
import pe.com.salon.salongestionapi.rrhh.repository.TurnoRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.util.List;

@RestController
@RequestMapping("/api/rrhh/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoRepository turnoRepository;

    @GetMapping
    public ResponseEntity<List<Turno>> getAll() {
        return ResponseEntity.ok(turnoRepository.findAll());
    }
    
    @GetMapping("/activos")
    public ResponseEntity<List<Turno>> getActivos() {
        return ResponseEntity.ok(turnoRepository.findByEstadoTrue());
    }

    @PostMapping
    public ResponseEntity<Turno> create(@RequestBody Turno turno) {
        turno.setEstado(true);
        return ResponseEntity.ok(turnoRepository.save(turno));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Turno> update(@PathVariable Long id, @RequestBody Turno request) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));
                
        turno.setNombre(request.getNombre());
        turno.setHoraEntrada(request.getHoraEntrada());
        turno.setHoraSalida(request.getHoraSalida());
        turno.setToleranciaMinutos(request.getToleranciaMinutos());
        turno.setEstado(request.getEstado());
        
        return ResponseEntity.ok(turnoRepository.save(turno));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));
        turno.setEstado(false);
        turnoRepository.save(turno);
        return ResponseEntity.ok().build();
    }
}
