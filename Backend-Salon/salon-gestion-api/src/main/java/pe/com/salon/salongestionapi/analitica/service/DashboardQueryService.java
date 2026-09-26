package pe.com.salon.salongestionapi.analitica.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.analitica.dto.*;
import pe.com.salon.salongestionapi.catalogo.entity.Producto;
import pe.com.salon.salongestionapi.catalogo.repository.ProductoRepository;
import pe.com.salon.salongestionapi.finanzas.repository.GastoCajaChicaRepository;
import pe.com.salon.salongestionapi.finanzas.repository.LiquidacionRepository;
import pe.com.salon.salongestionapi.operaciones.entity.Cliente;
import pe.com.salon.salongestionapi.operaciones.repository.ClienteRepository;
import pe.com.salon.salongestionapi.operaciones.repository.TicketDetalleRepository;
import pe.com.salon.salongestionapi.operaciones.repository.TicketRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardQueryService {

    private final TicketRepository ticketRepository;
    private final TicketDetalleRepository ticketDetalleRepository;
    private final GastoCajaChicaRepository gastoCajaChicaRepository;
    private final LiquidacionRepository liquidacionRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;

    // ------------ Ranking de Especialistas (GAP original) ------------

    public List<RankingEspecialistaDTO> obtenerRankingEspecialistas(Integer mes, Integer anio) {
        LocalDateTime inicio = YearMonth.of(anio, mes).atDay(1).atStartOfDay();
        LocalDateTime fin = YearMonth.of(anio, mes).atEndOfMonth().atTime(LocalTime.MAX);
        return ticketDetalleRepository.getRankingEspecialistas(inicio, fin);
    }

    // ------------ Margen Neto (GAP original) ------------

    public MargenNetoResponse calcularMargenNeto(Integer mes, Integer anio) {
        LocalDateTime inicio = YearMonth.of(anio, mes).atDay(1).atStartOfDay();
        LocalDateTime fin = YearMonth.of(anio, mes).atEndOfMonth().atTime(LocalTime.MAX);
        LocalDate inicioDate = YearMonth.of(anio, mes).atDay(1);
        LocalDate finDate = YearMonth.of(anio, mes).atEndOfMonth();

        BigDecimal ingresos = ticketRepository.sumTotalByFechaEmisionBetween(inicio, fin);
        if (ingresos == null) ingresos = BigDecimal.ZERO;

        BigDecimal gastos = gastoCajaChicaRepository.sumMontoByEstadoTrueAndFechaGastoBetween(inicio, fin);
        if (gastos == null) gastos = BigDecimal.ZERO;

        BigDecimal comisiones = liquidacionRepository.sumTotalComisionesByPagadoTrueAndFechaFinBetween(inicioDate, finDate);
        if (comisiones == null) comisiones = BigDecimal.ZERO;

        BigDecimal margen = ingresos.subtract(gastos).subtract(comisiones);

        return new MargenNetoResponse(ingresos, gastos, comisiones, margen,
                String.format("%02d/%d", mes, anio));
    }

    // ------------ GAP 9: Servicios más demandados ------------

    public List<ServicioDemandaDTO> obtenerServiciosMasDemandados(Integer mes, Integer anio) {
        LocalDateTime inicio = YearMonth.of(anio, mes).atDay(1).atStartOfDay();
        LocalDateTime fin = YearMonth.of(anio, mes).atEndOfMonth().atTime(LocalTime.MAX);
        return ticketDetalleRepository.getServiciosMasDemandados(inicio, fin);
    }

    // ------------ GAP 9: Desglose de ingresos (Servicios vs Productos) ------------

    public DesglosIngresosDTO obtenerDesglosIngresos(Integer mes, Integer anio) {
        LocalDateTime inicio = YearMonth.of(anio, mes).atDay(1).atStartOfDay();
        LocalDateTime fin = YearMonth.of(anio, mes).atEndOfMonth().atTime(LocalTime.MAX);

        BigDecimal porServicios = ticketDetalleRepository.sumIngresosPorServicios(inicio, fin);
        BigDecimal porProductos = ticketDetalleRepository.sumIngresosPorProductos(inicio, fin);
        if (porServicios == null) porServicios = BigDecimal.ZERO;
        if (porProductos == null) porProductos = BigDecimal.ZERO;

        return new DesglosIngresosDTO(porServicios, porProductos,
                porServicios.add(porProductos), String.format("%02d/%d", mes, anio));
    }

    // ------------ GAP 8: Alertas de stock bajo ------------

    public List<StockAlertaDTO> obtenerAlertasStock() {
        return productoRepository.findProductosConStockBajo().stream()
                .map(p -> new StockAlertaDTO(
                        p.getId(),
                        p.getNombre(),
                        p.getMarca(),
                        p.getStockActual(),
                        p.getStockMinimo(),
                        p.getStockMinimo() - p.getStockActual()
                ))
                .collect(Collectors.toList());
    }

    // ------------ GAP 7: Clientes que cumplen años en un mes ------------

    public List<Cliente> obtenerCumpleaniosDelMes(Integer mes) {
        return clienteRepository.findAll().stream()
                .filter(c -> c.getFechaNacimiento() != null
                        && c.getFechaNacimiento().getMonthValue() == mes
                        && Boolean.TRUE.equals(c.getEstado()))
                .collect(Collectors.toList());
    }
}
