package pe.com.salon.salongestionapi.rrhh.service;

import pe.com.salon.salongestionapi.rrhh.dto.IncentivoRequest;
import pe.com.salon.salongestionapi.rrhh.dto.IncentivoResponse;
import pe.com.salon.salongestionapi.rrhh.dto.MonitorComisionPageResponse;
import pe.com.salon.salongestionapi.rrhh.dto.MonitorComisionResponse;
import pe.com.salon.salongestionapi.rrhh.dto.LiquidacionResponse;
import pe.com.salon.salongestionapi.rrhh.dto.DetalleLiquidacionResponse;

import java.time.LocalDate;
import java.util.List;

public interface IncentivoService {
    List<IncentivoResponse> findAll();
    IncentivoResponse findById(Long id);
    IncentivoResponse create(IncentivoRequest request);
    IncentivoResponse update(Long id, IncentivoRequest request);
    void delete(Long id);
    List<IncentivoResponse> getActiveIncentivos();
    List<MonitorComisionResponse> getMonitorComisiones();
    List<MonitorComisionResponse> getMonitorComisiones(LocalDate fechaInicio, LocalDate fechaFin);
    MonitorComisionPageResponse getMonitorComisionesPaginado(LocalDate fechaInicio, LocalDate fechaFin, String search, int page, int size);
    List<LiquidacionResponse> getLiquidaciones(LocalDate fechaInicio, LocalDate fechaFin);
    LiquidacionResponse calcularLiquidacionEmpleado(Long empleadoId, LocalDate fechaInicio, LocalDate fechaFin);
    List<DetalleLiquidacionResponse> getDetalleLiquidacion(Long empleadoId, LocalDate fechaInicio, LocalDate fechaFin);
}
