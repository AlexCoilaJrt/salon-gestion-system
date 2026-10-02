package pe.com.salon.salongestionapi.operaciones.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.catalogo.entity.Producto;
import pe.com.salon.salongestionapi.catalogo.entity.Servicio;
import pe.com.salon.salongestionapi.catalogo.repository.ProductoRepository;
import pe.com.salon.salongestionapi.catalogo.repository.ServicioRepository;
import pe.com.salon.salongestionapi.operaciones.dto.TicketDetalleRequest;
import pe.com.salon.salongestionapi.operaciones.dto.TicketDetalleResponse;
import pe.com.salon.salongestionapi.operaciones.dto.TicketRequest;
import pe.com.salon.salongestionapi.operaciones.dto.TicketResponse;
import pe.com.salon.salongestionapi.operaciones.entity.Cliente;
import pe.com.salon.salongestionapi.operaciones.entity.SesionCaja;
import pe.com.salon.salongestionapi.operaciones.entity.Ticket;
import pe.com.salon.salongestionapi.operaciones.entity.TicketDetalle;
import pe.com.salon.salongestionapi.operaciones.repository.ClienteRepository;
import pe.com.salon.salongestionapi.operaciones.repository.SesionCajaRepository;
import pe.com.salon.salongestionapi.operaciones.repository.TicketRepository;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;
import pe.com.salon.salongestionapi.rrhh.repository.EmpleadoRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;
import pe.com.salon.salongestionapi.fidelizacion.service.FidelizacionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final SesionCajaRepository sesionCajaRepository;
    private final ClienteRepository clienteRepository;
    private final ServicioRepository servicioRepository;
    private final ProductoRepository productoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final FidelizacionService fidelizacionService;

    // Puntos que se otorgan por cada sol gastado (GAP 6: Fidelización)
    private static final int PUNTOS_POR_SOL = 1;

    @Transactional
    public TicketResponse emitirTicket(TicketRequest request) {
        // 1. Validar que la caja esté abierta
        SesionCaja sesionActiva = sesionCajaRepository.findByEstadoTrue()
                .orElseThrow(() -> new RuntimeException("No se puede emitir ticket porque no hay una caja abierta"));

        // 2. Buscar al cliente (Opcional - con Fallback para Base de Datos)
        Cliente cliente = null;
        if (request.getClienteId() != null) {
            cliente = clienteRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + request.getClienteId()));
        } else {
            // Creamos o buscamos un cliente genérico porque la BD exige un cliente_id
            cliente = clienteRepository.findById(1L).orElseGet(() -> {
                Cliente fallback = new Cliente();
                fallback.setNombres("Público");
                fallback.setApellidos("General");
                return clienteRepository.save(fallback);
            });
        }

        // 3. Preparar cabecera del Ticket
        Ticket ticket = new Ticket();
        ticket.setFechaEmision(LocalDateTime.now());
        ticket.setMetodoPago(request.getMetodoPago());
        ticket.setCliente(cliente);
        ticket.setNombreClienteNoRegistrado(request.getNombreClienteNoRegistrado());
        ticket.setSesionCaja(sesionActiva);

        BigDecimal totalVenta = BigDecimal.ZERO;

        // 4. Procesar Detalles
        for (TicketDetalleRequest detReq : request.getDetalles()) {
            TicketDetalle detalle = new TicketDetalle();
            detalle.setCantidad(detReq.getCantidad());
            BigDecimal precioUnitario = BigDecimal.ZERO;

            // Es un servicio
            if (detReq.getServicioId() != null) {
                Servicio servicio = servicioRepository.findById(detReq.getServicioId())
                        .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + detReq.getServicioId()));

                // GAP 2: Si es un servicio, el empleado es obligatorio
                if (detReq.getEmpleadoId() == null) {
                    throw new RuntimeException("El empleado es obligatorio cuando el detalle es un servicio (servicioId: " + detReq.getServicioId() + ")");
                }

                detalle.setServicio(servicio);
                precioUnitario = servicio.getPrecioBase();

            // Es un producto
            } else if (detReq.getProductoId() != null) {
                Producto producto = productoRepository.findById(detReq.getProductoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + detReq.getProductoId()));

                // Validar Stock
                if (producto.getStockActual() < detReq.getCantidad()) {
                    throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre());
                }
                // Descontar Stock
                producto.setStockActual(producto.getStockActual() - detReq.getCantidad());
                productoRepository.save(producto);

                detalle.setProducto(producto);
                precioUnitario = producto.getPrecioVenta();
            } else {
                throw new RuntimeException("Cada detalle debe tener un servicioId o un productoId");
            }

            // Relación con el empleado (Quien ejecutó el servicio / atendió la venta)
            if (detReq.getEmpleadoId() != null) {
                Empleado empleado = empleadoRepository.findById(detReq.getEmpleadoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + detReq.getEmpleadoId()));
                detalle.setEmpleado(empleado);
            }

            // Cálculos matemáticos
            detalle.setPrecioUnitario(precioUnitario);
            BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(detReq.getCantidad()));
            detalle.setSubtotal(subtotal);
            totalVenta = totalVenta.add(subtotal);

            ticket.addDetalle(detalle);
        }

        // 5. Guardar Ticket
        ticket.setTotal(totalVenta);
        Ticket guardado = ticketRepository.save(ticket);

        // 6. GAP 6: Actualizar puntos de fidelización y fecha de última visita (Sólo si hay cliente)
        if (cliente != null && !cliente.getId().equals(1L)) {
            int puntosGanados = totalVenta.intValue() * PUNTOS_POR_SOL;
            cliente.setPuntosFidelizacion(cliente.getPuntosFidelizacion() + puntosGanados);
            cliente.setFechaUltimaVisita(LocalDate.now());
            clienteRepository.save(cliente);

            // Procesar Cartillas de Fidelización (Sellos automáticos): Máximo 1 sello por ticket
            // Si el cliente eligió a qué servicio aplicarlo, usamos ese. Si no, tomamos el más caro por defecto.
            Long servicioPrioridadId = request.getServicioSelloId();

            List<TicketDetalle> detallesFiltrados = guardado.getDetalles().stream()
                .filter(d -> d.getServicio() != null)
                .collect(Collectors.toList());

            if (servicioPrioridadId != null) {
                // Poner el servicio elegido por el cliente de primero en la lista
                detallesFiltrados.sort((d1, d2) -> {
                    if (d1.getServicio().getId().equals(servicioPrioridadId)) return -1;
                    if (d2.getServicio().getId().equals(servicioPrioridadId)) return 1;
                    return d2.getPrecioUnitario().compareTo(d1.getPrecioUnitario()); // Fallback: ordenar por precio
                });
            } else {
                // Si no eligió nada, simplemente ordenar por el más caro
                detallesFiltrados.sort((d1, d2) -> d2.getPrecioUnitario().compareTo(d1.getPrecioUnitario()));
            }

            for (TicketDetalle d : detallesFiltrados) {
                boolean selloAgregado = fidelizacionService.procesarPagoServicio(cliente.getId(), d.getServicio().getId());
                if (selloAgregado) {
                    break; // Solo otorgamos 1 sello por visita/ticket
                }
            }

            // Procesar canje de premios en bloque (al confirmar el ticket)
            if (request.getPremiosFidelizacionIds() != null && !request.getPremiosFidelizacionIds().isEmpty()) {
                for (Long cartillaId : request.getPremiosFidelizacionIds()) {
                    fidelizacionService.canjearPremio(cliente.getId(), cartillaId);
                }
            }
        }

        return mapToResponse(guardado);
    }

    public List<TicketResponse> obtenerTicketsCajaActual() {
        SesionCaja sesionActiva = sesionCajaRepository.findByEstadoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna caja abierta actualmente"));
        
        List<Ticket> tickets = ticketRepository.findBySesionCajaId(sesionActiva.getId());
        return tickets.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<TicketResponse> obtenerTodosTickets() {
        return ticketRepository.findAllByOrderByFechaEmisionDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TicketResponse anularTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado con id: " + ticketId));

        if (!Boolean.TRUE.equals(ticket.getActivo())) {
            throw new RuntimeException("El ticket ya se encuentra anulado");
        }

        ticket.setActivo(false);

        // Si hay productos, devolver el stock
        for (TicketDetalle detalle : ticket.getDetalles()) {
            if (detalle.getProducto() != null) {
                Producto p = detalle.getProducto();
                p.setStockActual(p.getStockActual() + detalle.getCantidad());
                productoRepository.save(p);
            }
        }

        // Restar puntos de fidelización si aplica
        if (ticket.getCliente() != null) {
            int puntosPerdidos = ticket.getTotal().intValue() * PUNTOS_POR_SOL;
            int puntosNuevos = ticket.getCliente().getPuntosFidelizacion() - puntosPerdidos;
            ticket.getCliente().setPuntosFidelizacion(Math.max(0, puntosNuevos));
            clienteRepository.save(ticket.getCliente());
        }

        ticketRepository.save(ticket);
        return mapToResponse(ticket);
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        TicketResponse res = new TicketResponse();
        res.setId(ticket.getId());
        res.setFechaEmision(ticket.getFechaEmision());
        res.setMetodoPago(ticket.getMetodoPago());
        res.setTotal(ticket.getTotal());
        
        if (ticket.getCliente() != null) {
            res.setClienteId(ticket.getCliente().getId());
            res.setClienteNombreCompleto(ticket.getCliente().getNombres() + " " + ticket.getCliente().getApellidos());
        } else {
            res.setClienteNombreCompleto(ticket.getNombreClienteNoRegistrado() != null && !ticket.getNombreClienteNoRegistrado().isBlank() ? ticket.getNombreClienteNoRegistrado() : "Público General");
        }
        res.setSesionCajaId(ticket.getSesionCaja().getId());
        res.setActivo(ticket.getActivo());

        List<TicketDetalleResponse> detallesRes = ticket.getDetalles().stream().map(d -> {
            TicketDetalleResponse dRes = new TicketDetalleResponse();
            dRes.setId(d.getId());
            dRes.setCantidad(d.getCantidad());
            dRes.setPrecioUnitario(d.getPrecioUnitario());
            dRes.setSubtotal(d.getSubtotal());

            if (d.getServicio() != null) {
                dRes.setServicioId(d.getServicio().getId());
                dRes.setServicioNombre(d.getServicio().getNombre());
            }
            if (d.getProducto() != null) {
                dRes.setProductoId(d.getProducto().getId());
                dRes.setProductoNombre(d.getProducto().getNombre());
            }
            if (d.getEmpleado() != null) {
                dRes.setEmpleadoId(d.getEmpleado().getId());
                dRes.setEmpleadoNombreCompleto(d.getEmpleado().getNombres() + " " + d.getEmpleado().getApellidos());
                if (d.getEmpleado().getEspecialidades() != null) {
                    dRes.setEmpleadoEspecialidades(d.getEmpleado().getEspecialidades().stream()
                            .map(pe.com.salon.salongestionapi.rrhh.entity.Especialidad::getNombre)
                            .collect(Collectors.toList()));
                }
            }
            return dRes;
        }).collect(Collectors.toList());

        res.setDetalles(detallesRes);
        return res;
    }
}

