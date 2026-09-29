package pe.com.salon.salongestionapi.rrhh.service;

import pe.com.salon.salongestionapi.rrhh.dto.ComisionRequest;
import pe.com.salon.salongestionapi.rrhh.dto.ComisionResponse;
import java.util.List;

public interface ComisionService {
    List<ComisionResponse> findAll();
    ComisionResponse findById(Long id);
    ComisionResponse create(ComisionRequest request);
    ComisionResponse update(Long id, ComisionRequest request);
    void delete(Long id);
    List<ComisionResponse> findByEmpleadoId(Long empleadoId);
}
