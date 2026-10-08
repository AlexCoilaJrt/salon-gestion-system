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

        BigDecimal porServicios = ticketDetalleRepository.sumIngresosPorServicios(inicio, fin);
        BigDecimal porProductos = ticketDetalleRepository.sumIngresosPorProductos(inicio, fin);
        if (porServicios == null) porServicios = BigDecimal.ZERO;
        if (porProductos == null) porProductos = BigDecimal.ZERO;

        return new MargenNetoResponse(ingresos, porServicios, porProductos, gastos, comisiones, margen,
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

    // ------------ NUEVO REPORTE: Movimientos detallados ------------
    public List<ReporteVentaDetalleDTO> obtenerReporteVentasDetalle(String fechaInicioStr, String fechaFinStr) {
        LocalDateTime inicio = LocalDate.parse(fechaInicioStr).atStartOfDay();
        LocalDateTime fin = LocalDate.parse(fechaFinStr).atTime(LocalTime.MAX);

        return ticketDetalleRepository.findDetallesPorRangoFechas(inicio, fin).stream().map(td -> {
            ReporteVentaDetalleDTO dto = new ReporteVentaDetalleDTO();
            dto.setTicketId(td.getTicket().getId());
            dto.setFechaHora(td.getTicket().getFechaEmision());
            
            if (td.getTicket().getCliente() != null) {
                dto.setClienteNombre(td.getTicket().getCliente().getNombres() + " " + td.getTicket().getCliente().getApellidos());
            } else {
                dto.setClienteNombre("Público General");
            }
            
            if (td.getEmpleado() != null) {
                dto.setEmpleadoNombre(td.getEmpleado().getNombres() + " " + td.getEmpleado().getApellidos());
            } else {
                dto.setEmpleadoNombre("Sin asignar");
            }
            
            dto.setCantidad(td.getCantidad());
            dto.setPrecioUnitario(td.getPrecioUnitario());
            dto.setSubtotal(td.getSubtotal());
            dto.setEstadoTicket(Boolean.TRUE.equals(td.getTicket().getActivo()) ? "EMITIDO" : "ANULADO");
            
            if (td.getServicio() != null) {
                dto.setTipoItem("SERVICIO");
                dto.setItemNombre(td.getServicio().getNombre());
            } else if (td.getProducto() != null) {
                dto.setItemNombre(td.getProducto().getNombre());
                if (Boolean.TRUE.equals(td.getProducto().getUsoInterno())) {
                    dto.setTipoItem("INSUMO_INTERNO");
                } else {
                    dto.setTipoItem("PRODUCTO_RETAIL");
                }
            } else {
                dto.setTipoItem("DESCONOCIDO");
                dto.setItemNombre("Ítem Desconocido");
            }
            
            return dto;
        }).collect(Collectors.toList());
    }
}
