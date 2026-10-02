package pe.com.salon.salongestionapi.fidelizacion.service;

import pe.com.salon.salongestionapi.fidelizacion.dto.CartillaDTO;
import pe.com.salon.salongestionapi.fidelizacion.dto.ClienteCartillaDTO;

import java.util.List;

public interface FidelizacionService {
    CartillaDTO crearCartilla(CartillaDTO request);
    List<CartillaDTO> listarCartillas();
    
    // Core logic
    boolean procesarPagoServicio(Long clienteId, Long servicioId);
    
    List<ClienteCartillaDTO> listarCartillasPorCliente(Long clienteId);
    List<ClienteCartillaDTO> listarPremiosDisponibles(Long clienteId);
    void canjearPremio(Long clienteId, Long cartillaId);
    
    // Para ver el progreso global
    List<ClienteCartillaDTO> listarTodasCartillasClientes();
}
