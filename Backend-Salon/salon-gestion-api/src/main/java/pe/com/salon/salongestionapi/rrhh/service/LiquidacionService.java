package pe.com.salon.salongestionapi.rrhh.service;

import pe.com.salon.salongestionapi.rrhh.dto.LiquidacionRequest;
import pe.com.salon.salongestionapi.rrhh.dto.LiquidacionResponse;

import java.util.List;

public interface LiquidacionService {
    LiquidacionResponse crearLiquidacion(LiquidacionRequest request);
    List<LiquidacionResponse> obtenerPorEmpleado(Long empleadoId);
    List<LiquidacionResponse> obtenerTodas();
    LiquidacionResponse actualizarEstado(Long id, String estado);
}
