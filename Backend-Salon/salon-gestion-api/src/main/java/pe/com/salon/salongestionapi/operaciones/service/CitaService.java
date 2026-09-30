package pe.com.salon.salongestionapi.operaciones.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.catalogo.entity.Servicio;
import pe.com.salon.salongestionapi.catalogo.repository.ServicioRepository;
import pe.com.salon.salongestionapi.operaciones.dto.CitaRequest;
import pe.com.salon.salongestionapi.operaciones.dto.CitaResponse;
import pe.com.salon.salongestionapi.operaciones.entity.Cita;
import pe.com.salon.salongestionapi.operaciones.entity.Cliente;
import pe.com.salon.salongestionapi.operaciones.repository.CitaRepository;
import pe.com.salon.salongestionapi.operaciones.repository.ClienteRepository;
import pe.com.salon.salongestionapi.catalogo.entity.Producto;
import pe.com.salon.salongestionapi.catalogo.repository.ProductoRepository;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;
import pe.com.salon.salongestionapi.rrhh.repository.EmpleadoRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaRepository citaRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ServicioRepository servicioRepository;
    private final ProductoRepository productoRepository;

    public List<CitaResponse> listarTodas() {
        return citaRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public CitaResponse obtenerPorId(Long id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + id));
        return mapToResponse(cita);
    }

    public CitaResponse crearCita(CitaRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + request.getClienteId()));
        
        Empleado empleado = empleadoRepository.findById(request.getEmpleadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + request.getEmpleadoId()));
        
        Servicio servicio = servicioRepository.findById(request.getServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + request.getServicioId()));

        Cita cita = new Cita();
        cita.setFechaHora(request.getFechaHora());
        cita.setEstado(request.getEstado());
        cita.setAdelanto(request.getAdelanto());
        cita.setMetodoPago(request.getMetodoPago());
        cita.setNotas(request.getNotas());
        cita.setCliente(cliente);
        cita.setEmpleado(empleado);
        cita.setServicio(servicio);

        if (request.getProductosIds() != null && !request.getProductosIds().isEmpty()) {
            List<Producto> productos = productoRepository.findAllById(request.getProductosIds());
            cita.setProductos(productos);
        } else {
            cita.setProductos(new ArrayList<>());
        }

        Cita guardada = citaRepository.save(cita);
        return mapToResponse(guardada);
    }

    public CitaResponse actualizarCita(Long id, CitaRequest request) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + id));

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + request.getClienteId()));
        
        Empleado empleado = empleadoRepository.findById(request.getEmpleadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + request.getEmpleadoId()));
        
        Servicio servicio = servicioRepository.findById(request.getServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + request.getServicioId()));

        cita.setFechaHora(request.getFechaHora());
        cita.setEstado(request.getEstado());
        cita.setAdelanto(request.getAdelanto());
        cita.setMetodoPago(request.getMetodoPago());
        cita.setNotas(request.getNotas());
        cita.setCliente(cliente);
        cita.setEmpleado(empleado);
        cita.setServicio(servicio);

        if (request.getProductosIds() != null && !request.getProductosIds().isEmpty()) {
            List<Producto> productos = productoRepository.findAllById(request.getProductosIds());
            cita.setProductos(productos);
        } else {
            cita.setProductos(new ArrayList<>());
        }

        Cita actualizada = citaRepository.save(cita);
        return mapToResponse(actualizada);
    }

    public void eliminarCita(Long id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + id));
        
        citaRepository.delete(cita); // En Citas usualmente sí se puede hacer hard delete o pasarlas a CANCELADA
    }

    private CitaResponse mapToResponse(Cita cita) {
        CitaResponse response = new CitaResponse();
        response.setId(cita.getId());
        response.setFechaHora(cita.getFechaHora());
        response.setEstado(cita.getEstado());
        response.setAdelanto(cita.getAdelanto());
        response.setMetodoPago(cita.getMetodoPago());
        response.setNotas(cita.getNotas());
        
        response.setClienteId(cita.getCliente().getId());
        response.setClienteNombreCompleto(cita.getCliente().getNombres() + " " + cita.getCliente().getApellidos());
        
        response.setEmpleadoId(cita.getEmpleado().getId());
        response.setEmpleadoNombreCompleto(cita.getEmpleado().getNombres() + " " + cita.getEmpleado().getApellidos());
        
        response.setServicioId(cita.getServicio().getId());
        response.setServicioNombre(cita.getServicio().getNombre());
        
        if (cita.getProductos() != null) {
            response.setProductosIds(cita.getProductos().stream().map(Producto::getId).collect(Collectors.toList()));
        } else {
            response.setProductosIds(new ArrayList<>());
        }
        
        return response;
    }
}
