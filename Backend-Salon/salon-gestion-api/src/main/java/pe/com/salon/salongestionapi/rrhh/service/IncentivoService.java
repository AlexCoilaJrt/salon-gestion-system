package pe.com.salon.salongestionapi.rrhh.service;

import pe.com.salon.salongestionapi.rrhh.dto.IncentivoRequest;
import pe.com.salon.salongestionapi.rrhh.dto.IncentivoResponse;

import java.util.List;

public interface IncentivoService {
    List<IncentivoResponse> findAll();
    IncentivoResponse findById(Long id);
    IncentivoResponse create(IncentivoRequest request);
    IncentivoResponse update(Long id, IncentivoRequest request);
    void delete(Long id);
    List<IncentivoResponse> getActiveIncentivos();
    List<pe.com.salon.salongestionapi.rrhh.dto.MonitorComisionResponse> getMonitorComisiones();
}
