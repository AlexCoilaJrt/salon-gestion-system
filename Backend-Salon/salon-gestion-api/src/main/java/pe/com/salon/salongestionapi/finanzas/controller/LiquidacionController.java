package pe.com.salon.salongestionapi.finanzas.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.finanzas.dto.LiquidacionRequest;
import pe.com.salon.salongestionapi.finanzas.dto.LiquidacionResponse;
import pe.com.salon.salongestionapi.finanzas.service.NominaService;

@RestController
@RequestMapping("/api/finanzas/liquidaciones")
@RequiredArgsConstructor
public class LiquidacionController {

    private final NominaService nominaService;

    @PostMapping("/generar")
    public ResponseEntity<LiquidacionResponse> generarLiquidacion(@Valid @RequestBody LiquidacionRequest request) {
        LiquidacionResponse nuevaLiquidacion = nominaService.generarLiquidacion(request);
        return new ResponseEntity<>(nuevaLiquidacion, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<LiquidacionResponse> pagarLiquidacion(@PathVariable Long id) {
        return ResponseEntity.ok(nominaService.pagarLiquidacion(id));
    }
}
