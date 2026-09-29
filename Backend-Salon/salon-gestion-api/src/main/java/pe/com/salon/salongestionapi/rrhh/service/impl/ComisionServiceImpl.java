package pe.com.salon.salongestionapi.rrhh.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.salon.salongestionapi.rrhh.dto.ComisionRequest;
import pe.com.salon.salongestionapi.rrhh.dto.ComisionResponse;
import pe.com.salon.salongestionapi.rrhh.entity.Comision;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;
import pe.com.salon.salongestionapi.rrhh.repository.ComisionRepository;
import pe.com.salon.salongestionapi.rrhh.repository.EmpleadoRepository;
import pe.com.salon.salongestionapi.rrhh.service.ComisionService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComisionServiceImpl implements ComisionService {

    private final ComisionRepository comisionRepository;
    private final EmpleadoRepository empleadoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ComisionResponse> findAll() {
        return comisionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ComisionResponse findById(Long id) {
        Comision comision = comisionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comisión no encontrada"));
        return mapToResponse(comision);
    }

    @Override
    @Transactional
    public ComisionResponse create(ComisionRequest request) {
        Empleado empleado = empleadoRepository.findById(request.getEmpleadoId())
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        Comision comision = Comision.builder()
                .empleado(empleado)
                .tipoComision(request.getTipoComision())
                .valor(request.getValor())
                .estado(request.getEstado() != null ? request.getEstado() : true)
                .build();

        return mapToResponse(comisionRepository.save(comision));
    }

    @Override
    @Transactional
    public ComisionResponse update(Long id, ComisionRequest request) {
        Comision comision = comisionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comisión no encontrada"));
        
        if (!comision.getEmpleado().getId().equals(request.getEmpleadoId())) {
            Empleado empleado = empleadoRepository.findById(request.getEmpleadoId())
                    .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
            comision.setEmpleado(empleado);
        }

        comision.setTipoComision(request.getTipoComision());
        comision.setValor(request.getValor());
        if (request.getEstado() != null) {
            comision.setEstado(request.getEstado());
        }

        return mapToResponse(comisionRepository.save(comision));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Comision comision = comisionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comisión no encontrada"));
        comision.setEstado(false);
        comisionRepository.save(comision);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComisionResponse> findByEmpleadoId(Long empleadoId) {
        return comisionRepository.findByEmpleadoId(empleadoId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ComisionResponse mapToResponse(Comision comision) {
        ComisionResponse response = new ComisionResponse();
        response.setId(comision.getId());
        response.setEmpleadoId(comision.getEmpleado().getId());
        response.setEmpleadoNombre(comision.getEmpleado().getNombres());
        response.setEmpleadoApellido(comision.getEmpleado().getApellidos());
        response.setTipoComision(comision.getTipoComision());
        response.setValor(comision.getValor());
        response.setEstado(comision.getEstado());
        response.setFechaRegistro(comision.getFechaRegistro());
        return response;
    }
}
