package pe.com.salon.salongestionapi.empresa.service;

import pe.com.salon.salongestionapi.empresa.dto.EmpresaResponse;
import pe.com.salon.salongestionapi.empresa.dto.EmpresaRequest;

public interface EmpresaService {
    EmpresaResponse obtenerEmpresaActiva();
    EmpresaResponse actualizarEmpresa(Long id, EmpresaRequest request);
}
