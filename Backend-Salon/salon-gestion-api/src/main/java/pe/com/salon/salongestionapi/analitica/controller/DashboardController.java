package pe.com.salon.salongestionapi.analitica.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.analitica.dto.*;
import pe.com.salon.salongestionapi.analitica.service.DashboardQueryService;
import pe.com.salon.salongestionapi.operaciones.entity.Cliente;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analitica/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardQueryService dashboardQueryService;

    @GetMapping("/ranking-especialistas")
    public ResponseEntity<List<RankingEspecialistaDTO>> obtenerRanking(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        if (mes == null) mes = LocalDate.now().getMonthValue();
        if (anio == null) anio = LocalDate.now().getYear();
        return ResponseEntity.ok(dashboardQueryService.obtenerRankingEspecialistas(mes, anio));
    }

    @GetMapping("/margen-neto")
    public ResponseEntity<MargenNetoResponse> obtenerMargenNeto(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        if (mes == null) mes = LocalDate.now().getMonthValue();
        if (anio == null) anio = LocalDate.now().getYear();
        return ResponseEntity.ok(dashboardQueryService.calcularMargenNeto(mes, anio));
    }

    // GAP 9: Servicios más demandados
    @GetMapping("/servicios-demandados")
    public ResponseEntity<List<ServicioDemandaDTO>> obtenerServiciosDemandados(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        if (mes == null) mes = LocalDate.now().getMonthValue();
        if (anio == null) anio = LocalDate.now().getYear();
        return ResponseEntity.ok(dashboardQueryService.obtenerServiciosMasDemandados(mes, anio));
    }

    // GAP 9: Desglose ingresos servicios vs productos
    @GetMapping("/desglose-ingresos")
    public ResponseEntity<DesglosIngresosDTO> obtenerDesglosIngresos(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        if (mes == null) mes = LocalDate.now().getMonthValue();
        if (anio == null) anio = LocalDate.now().getYear();
        return ResponseEntity.ok(dashboardQueryService.obtenerDesglosIngresos(mes, anio));
    }

    // GAP 8: Alertas de stock bajo
    @GetMapping("/alertas-stock")
    public ResponseEntity<List<StockAlertaDTO>> obtenerAlertasStock() {
        return ResponseEntity.ok(dashboardQueryService.obtenerAlertasStock());
    }

    // GAP 7: Clientes que cumplen años en el mes indicado
    @GetMapping("/cumpleanios")
    public ResponseEntity<List<Cliente>> obtenerCumpleaniosDelMes(
            @RequestParam(required = false) Integer mes) {
        if (mes == null) mes = LocalDate.now().getMonthValue();
        return ResponseEntity.ok(dashboardQueryService.obtenerCumpleaniosDelMes(mes));
    }

    @GetMapping("/reporte-ventas")
    public ResponseEntity<List<ReporteVentaDetalleDTO>> obtenerReporteVentasDetalle(
            @RequestParam("fechaInicio") String fechaInicio,
            @RequestParam("fechaFin") String fechaFin) {
        return ResponseEntity.ok(dashboardQueryService.obtenerReporteVentasDetalle(fechaInicio, fechaFin));
    }
}
