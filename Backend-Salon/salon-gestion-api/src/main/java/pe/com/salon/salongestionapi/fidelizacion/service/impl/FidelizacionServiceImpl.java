package pe.com.salon.salongestionapi.fidelizacion.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.salon.salongestionapi.catalogo.entity.Servicio;
import pe.com.salon.salongestionapi.catalogo.repository.ServicioRepository;
import pe.com.salon.salongestionapi.fidelizacion.dto.CartillaDTO;
import pe.com.salon.salongestionapi.fidelizacion.dto.ClienteCartillaDTO;
import pe.com.salon.salongestionapi.fidelizacion.entity.CartillaFidelizacion;
import pe.com.salon.salongestionapi.fidelizacion.entity.ClienteCartilla;
import pe.com.salon.salongestionapi.fidelizacion.repository.CartillaFidelizacionRepository;
import pe.com.salon.salongestionapi.fidelizacion.repository.ClienteCartillaRepository;
import pe.com.salon.salongestionapi.fidelizacion.service.FidelizacionService;
import pe.com.salon.salongestionapi.operaciones.entity.Cliente;
import pe.com.salon.salongestionapi.operaciones.repository.ClienteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FidelizacionServiceImpl implements FidelizacionService {

    private final CartillaFidelizacionRepository cartillaRepository;
    private final ClienteCartillaRepository clienteCartillaRepository;
    private final ServicioRepository servicioRepository;
    private final ClienteRepository clienteRepository;

    @Override
    @Transactional
    public CartillaDTO crearCartilla(CartillaDTO request) {
        Servicio servicio = servicioRepository.findById(request.getServicioId())
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        CartillaFidelizacion cartilla = CartillaFidelizacion.builder()
                .nombre(request.getNombre())
                .servicio(servicio)
                .metaSellos(request.getMetaSellos())
                .descuentoPremio(request.getDescuentoPremio())
                .estado(true)
                .build();

        cartilla = cartillaRepository.save(cartilla);
        return mapToDTO(cartilla);
    }

    @Override
    public List<CartillaDTO> listarCartillas() {
        return cartillaRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean procesarPagoServicio(Long clienteId, Long servicioId) {
        if (clienteId == null || servicioId == null) return false;

        // Check if there is an active loyalty program for this service
        CartillaFidelizacion cartillaActiva = cartillaRepository.findByServicioIdAndEstadoTrue(servicioId);
        if (cartillaActiva == null) return false;

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // Check if client already has this card in progress
        ClienteCartilla progress = clienteCartillaRepository
                .findByClienteIdAndCartillaFidelizacionIdAndCanjeadaFalse(clienteId, cartillaActiva.getId())
                .orElse(ClienteCartilla.builder()
                        .cliente(cliente)
                        .cartillaFidelizacion(cartillaActiva)
                        .sellosActuales(0)
                        .completada(false)
                        .canjeada(false)
                        .build());

        // Increment stamp only if not completed yet
        if (!progress.getCompletada()) {
            progress.setSellosActuales(progress.getSellosActuales() + 1);
            progress.setFechaUltimaActualizacion(LocalDateTime.now());
            
            if (progress.getSellosActuales() >= cartillaActiva.getMetaSellos()) {
                progress.setCompletada(true);
            }
            clienteCartillaRepository.save(progress);
            return true;
        }
        return false;
    }

    @Override
    public List<ClienteCartillaDTO> listarCartillasPorCliente(Long clienteId) {
        return clienteCartillaRepository.findByClienteId(clienteId).stream()
                .map(this::mapToClienteCartillaDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClienteCartillaDTO> listarTodasCartillasClientes() {
        return clienteCartillaRepository.findAll().stream()
                .map(this::mapToClienteCartillaDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClienteCartillaDTO> listarPremiosDisponibles(Long clienteId) {
        return clienteCartillaRepository.findByClienteIdAndCompletadaTrueAndCanjeadaFalse(clienteId).stream()
                .map(this::mapToClienteCartillaDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void canjearPremio(Long clienteId, Long cartillaId) {
        ClienteCartilla progress = clienteCartillaRepository
                .findByClienteIdAndCartillaFidelizacionIdAndCanjeadaFalse(clienteId, cartillaId)
                .orElseThrow(() -> new RuntimeException("No hay un premio disponible para esta cartilla"));

        if (!progress.getCompletada()) {
            throw new RuntimeException("La cartilla aún no está completada");
        }

        progress.setCanjeada(true);
        progress.setFechaUltimaActualizacion(LocalDateTime.now());
        clienteCartillaRepository.save(progress);
    }

    private CartillaDTO mapToDTO(CartillaFidelizacion entity) {
        return CartillaDTO.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .servicioId(entity.getServicio().getId())
                .servicioNombre(entity.getServicio().getNombre())
                .metaSellos(entity.getMetaSellos())
                .descuentoPremio(entity.getDescuentoPremio())
                .estado(entity.getEstado())
                .build();
    }

    private ClienteCartillaDTO mapToClienteCartillaDTO(ClienteCartilla entity) {
        return ClienteCartillaDTO.builder()
                .id(entity.getId())
                .clienteId(entity.getCliente().getId())
                .clienteNombre(entity.getCliente().getNombres() + " " + entity.getCliente().getApellidos())
                .cartillaId(entity.getCartillaFidelizacion().getId())
                .cartillaNombre(entity.getCartillaFidelizacion().getNombre())
                .servicioRequerido(entity.getCartillaFidelizacion().getServicio().getNombre())
                .metaSellos(entity.getCartillaFidelizacion().getMetaSellos())
                .sellosActuales(entity.getSellosActuales())
                .descuentoPremio(entity.getCartillaFidelizacion().getDescuentoPremio())
                .completada(entity.getCompletada())
                .canjeada(entity.getCanjeada())
                .fechaUltimaActualizacion(entity.getFechaUltimaActualizacion())
                .build();
    }
}
