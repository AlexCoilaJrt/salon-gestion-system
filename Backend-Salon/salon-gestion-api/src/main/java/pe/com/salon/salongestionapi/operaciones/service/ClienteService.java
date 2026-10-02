package pe.com.salon.salongestionapi.operaciones.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.operaciones.dto.ClienteRequest;
import pe.com.salon.salongestionapi.operaciones.dto.ClienteResponse;
import pe.com.salon.salongestionapi.operaciones.entity.Cliente;
import pe.com.salon.salongestionapi.operaciones.repository.ClienteRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public List<ClienteResponse> listarTodos() {
        return clienteRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ClienteResponse obtenerPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
        return mapToResponse(cliente);
    }

    public ClienteResponse crearCliente(ClienteRequest request) {
        Cliente cliente = new Cliente();
        cliente.setNombres(request.getNombres());
        cliente.setApellidos(request.getApellidos());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setEstado(true);

        Cliente guardado = clienteRepository.save(cliente);
        return mapToResponse(guardado);
    }

    public ClienteResponse actualizarCliente(Long id, ClienteRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        cliente.setNombres(request.getNombres());
        cliente.setApellidos(request.getApellidos());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());

        Cliente actualizado = clienteRepository.save(cliente);
        return mapToResponse(actualizado);
    }

    public void eliminarCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
        
        cliente.setEstado(false);
        clienteRepository.save(cliente);
    }

    private ClienteResponse mapToResponse(Cliente cliente) {
        ClienteResponse response = new ClienteResponse();
        response.setId(cliente.getId());
        response.setNombres(cliente.getNombres());
        response.setApellidos(cliente.getApellidos());
        response.setTelefono(cliente.getTelefono());
        response.setEmail(cliente.getEmail());
        response.setFechaNacimiento(cliente.getFechaNacimiento());
        response.setEstado(cliente.getEstado());
        return response;
    }
}
