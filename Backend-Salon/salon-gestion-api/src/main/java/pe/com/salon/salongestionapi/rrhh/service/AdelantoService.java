package pe.com.salon.salongestionapi.rrhh.service;

import pe.com.salon.salongestionapi.rrhh.dto.AdelantoRequest;
import pe.com.salon.salongestionapi.rrhh.dto.AdelantoResponse;

import java.time.LocalDate;
import java.util.List;

public interface AdelantoService {
    AdelantoResponse registrarAdelanto(AdelantoRequest request);
    List<AdelantoResponse> obtenerAdelantosPorEmpleadoYPeriodo(Long empleadoId, LocalDate inicio, LocalDate fin);
    AdelantoResponse anularAdelanto(Long id);
    AdelantoResponse actualizarAdelanto(Long id, AdelantoRequest request);
    void eliminarAdelanto(Long id);
}

