package pe.com.salon.salongestionapi.empresa.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.empresa.dto.EmpresaRequest;
import pe.com.salon.salongestionapi.empresa.dto.EmpresaResponse;
import pe.com.salon.salongestionapi.empresa.entity.Empresa;
import pe.com.salon.salongestionapi.empresa.repository.EmpresaRepository;
import pe.com.salon.salongestionapi.empresa.service.EmpresaService;
import pe.com.salon.salongestionapi.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaRepository empresaRepository;

    @Override
    public EmpresaResponse obtenerEmpresaActiva() {
        Empresa empresa = empresaRepository.findFirstByActivoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay una empresa activa configurada"));
                
        return mapToResponse(empresa);
    }

    @Override
    public EmpresaResponse actualizarEmpresa(Long id, EmpresaRequest request) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + id));

        empresa.setNombreComercial(request.nombreComercial());
        empresa.setRazonSocial(request.razonSocial());
        empresa.setRuc(request.ruc());
        empresa.setDireccion(request.direccion());
        empresa.setTelefono(request.telefono());
        empresa.setEmail(request.email());
        empresa.setLogoUrl(request.logoUrl());

        Empresa empresaActualizada = empresaRepository.save(empresa);
        return mapToResponse(empresaActualizada);
    }

    private EmpresaResponse mapToResponse(Empresa empresa) {
        return new EmpresaResponse(
                empresa.getId(),
                empresa.getNombreComercial(),
                empresa.getRazonSocial(),
                empresa.getRuc(),
                empresa.getDireccion(),
                empresa.getTelefono(),
                empresa.getEmail(),
                empresa.getLogoUrl()
        );
    }
}
