package pe.com.salon.salongestionapi.rrhh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorComisionPageResponse {
    private List<MonitorComisionResponse> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean isLast;

    // Resumen métricas globales del período (KPIs)
    private BigDecimal totalVentasPeriodo;
    private BigDecimal totalComisionesPeriodo;
    private long empleadosConVentas;
    private long totalEmpleados;
}

