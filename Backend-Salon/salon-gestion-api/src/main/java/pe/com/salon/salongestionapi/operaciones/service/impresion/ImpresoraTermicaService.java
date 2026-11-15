package pe.com.salon.salongestionapi.operaciones.service.impresion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.salon.salongestionapi.empresa.entity.Empresa;
import pe.com.salon.salongestionapi.empresa.repository.EmpresaRepository;
import pe.com.salon.salongestionapi.fidelizacion.entity.ClienteCartilla;
import pe.com.salon.salongestionapi.fidelizacion.repository.ClienteCartillaRepository;
import pe.com.salon.salongestionapi.operaciones.dto.impresion.ImpresoraConfigDTO;
import pe.com.salon.salongestionapi.operaciones.dto.impresion.ImpresoraTestResultDTO;
import pe.com.salon.salongestionapi.operaciones.entity.Ticket;
import pe.com.salon.salongestionapi.operaciones.entity.TicketDetalle;
import pe.com.salon.salongestionapi.operaciones.repository.TicketRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;
import pe.com.salon.salongestionapi.shared.service.ConfiguracionService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImpresoraTermicaService {

    private final ConfiguracionService configuracionService;
    private final EmpresaRepository empresaRepository;
    private final TicketRepository ticketRepository;
    private final ClienteCartillaRepository clienteCartillaRepository;

    // Comandos ESC/POS estándar
    private static final byte[] ESC_INIT = new byte[]{0x1B, 0x40};               // ESC @
    private static final byte[] ESC_ALIGN_LEFT = new byte[]{0x1B, 0x61, 0x00};   // ESC a 0
    private static final byte[] ESC_ALIGN_CENTER = new byte[]{0x1B, 0x61, 0x01}; // ESC a 1
    private static final byte[] ESC_ALIGN_RIGHT = new byte[]{0x1B, 0x61, 0x02};  // ESC a 2
    private static final byte[] ESC_BOLD_ON = new byte[]{0x1B, 0x45, 0x01};      // ESC E 1
    private static final byte[] ESC_BOLD_OFF = new byte[]{0x1B, 0x45, 0x00};     // ESC E 0
    private static final byte[] GS_SIZE_DOUBLE = new byte[]{0x1D, 0x21, 0x11};   // GS ! 0x11 (doble alto y ancho)
    private static final byte[] GS_SIZE_NORMAL = new byte[]{0x1D, 0x21, 0x00};   // GS ! 0x00 (normal)
    private static final byte[] GS_CUT_PARTIAL = new byte[]{0x1D, 0x56, 0x42, 0x00}; // GS V 66 0 (corte parcial)
    private static final byte[] ESC_DRAWER_PULSE = new byte[]{0x1B, 0x70, 0x00, 0x19, (byte) 0xFA}; // Pulso gaveta 24V

    public ImpresoraConfigDTO getConfiguracion() {
        return ImpresoraConfigDTO.builder()
                .ip(configuracionService.getStringOrDefault(ConfiguracionService.IMPRESORA_TICKET_IP, "192.168.123.100"))
                .puerto(configuracionService.getIntegerOrDefault(ConfiguracionService.IMPRESORA_TICKET_PUERTO, 9100))
                .autoImprimirAlCobrar(configuracionService.getBooleanOrDefault(ConfiguracionService.IMPRESORA_TICKET_AUTO_IMPRIMIR, true))
                .autoCorte(configuracionService.getBooleanOrDefault(ConfiguracionService.IMPRESORA_TICKET_AUTO_CORTE, true))
                .abrirGaveta(configuracionService.getBooleanOrDefault(ConfiguracionService.IMPRESORA_TICKET_ABRIR_GAVETA, true))
                .mensajePie(configuracionService.getStringOrDefault(ConfiguracionService.IMPRESORA_TICKET_MENSAJE_PIE, "¡Gracias por su preferencia!"))
                .anchoPapelMm(configuracionService.getIntegerOrDefault(ConfiguracionService.IMPRESORA_TICKET_ANCHO_MM, 80))
                .build();
    }

    public ImpresoraConfigDTO guardarConfiguracion(ImpresoraConfigDTO dto) {
        String ipLimpia = normalizarIp(dto.getIp());
        configuracionService.actualizar(ConfiguracionService.IMPRESORA_TICKET_IP, ipLimpia);
        configuracionService.actualizar(ConfiguracionService.IMPRESORA_TICKET_PUERTO, String.valueOf(dto.getPuerto() != null ? dto.getPuerto() : 9100));
        configuracionService.actualizar(ConfiguracionService.IMPRESORA_TICKET_AUTO_IMPRIMIR, String.valueOf(dto.getAutoImprimirAlCobrar() != null ? dto.getAutoImprimirAlCobrar() : true));
        configuracionService.actualizar(ConfiguracionService.IMPRESORA_TICKET_AUTO_CORTE, String.valueOf(dto.getAutoCorte() != null ? dto.getAutoCorte() : true));
        configuracionService.actualizar(ConfiguracionService.IMPRESORA_TICKET_ABRIR_GAVETA, String.valueOf(dto.getAbrirGaveta() != null ? dto.getAbrirGaveta() : true));
        configuracionService.actualizar(ConfiguracionService.IMPRESORA_TICKET_MENSAJE_PIE, dto.getMensajePie() != null ? dto.getMensajePie() : "¡Gracias por su preferencia!");
        configuracionService.actualizar(ConfiguracionService.IMPRESORA_TICKET_ANCHO_MM, String.valueOf(dto.getAnchoPapelMm() != null ? dto.getAnchoPapelMm() : 80));
        return getConfiguracion();
    }

    public ImpresoraTestResultDTO probarConexion(String ipOpt, Integer puertoOpt) {
        ImpresoraConfigDTO config = getConfiguracion();
        String ip = normalizarIp(ipOpt != null && !ipOpt.isBlank() ? ipOpt : config.getIp());
        int puerto = (puertoOpt != null && puertoOpt > 0) ? puertoOpt : (config.getPuerto() != null ? config.getPuerto() : 9100);

        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, puerto), 2500);
            long latencia = System.currentTimeMillis() - start;
            log.info("Conexión exitosa a impresora térmica en {}:{} en {} ms", ip, puerto, latencia);
            return ImpresoraTestResultDTO.builder()
                    .conectado(true)
                    .ip(ip)
                    .puerto(puerto)
                    .latenciaMs(latencia)
                    .mensaje("Conexión establecida correctamente con la impresora (" + latencia + " ms).")
                    .build();
        } catch (Exception e) {
            log.warn("Fallo de conexión a impresora {}:{}: {}", ip, puerto, e.getMessage());
            return ImpresoraTestResultDTO.builder()
                    .conectado(false)
                    .ip(ip)
                    .puerto(puerto)
                    .latenciaMs(0L)
                    .mensaje("No se pudo conectar a " + ip + ":" + puerto + ". Verifica que la impresora esté encendida, con cable de red conectado y en la misma subred.")
                    .build();
        }
    }

    public ImpresoraTestResultDTO imprimirTicketPrueba(String ipOpt, Integer puertoOpt) {
        ImpresoraConfigDTO config = getConfiguracion();
        String ip = normalizarIp(ipOpt != null && !ipOpt.isBlank() ? ipOpt : config.getIp());
        int puerto = (puertoOpt != null && puertoOpt > 0) ? puertoOpt : (config.getPuerto() != null ? config.getPuerto() : 9100);

        try {
            byte[] bytesTicket = generarPayloadTicketPrueba(config, ip, puerto);
            enviarSocket(ip, puerto, bytesTicket);
            return ImpresoraTestResultDTO.builder()
                    .conectado(true)
                    .ip(ip)
                    .puerto(puerto)
                    .mensaje("Ticket de prueba impreso correctamente en " + ip + ":" + puerto)
                    .build();
        } catch (Exception e) {
            log.error("Error al imprimir ticket de prueba en {}:{}: {}", ip, puerto, e.getMessage());
            return ImpresoraTestResultDTO.builder()
                    .conectado(false)
                    .ip(ip)
                    .puerto(puerto)
                    .mensaje("Error al imprimir: " + e.getMessage())
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public void imprimirTicketVenta(Long ticketId) {
        Ticket ticket = ticketRepository.findByIdWithDetalles(ticketId)
                .orElseGet(() -> ticketRepository.findById(ticketId)
                        .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado con ID: " + ticketId)));

        ImpresoraConfigDTO config = getConfiguracion();
        String ip = normalizarIp(config.getIp());
        int puerto = config.getPuerto() != null ? config.getPuerto() : 9100;

        Empresa empresa = empresaRepository.findFirstByActivoTrue().orElse(null);

        byte[] payload = generarPayloadTicketVenta(ticket, empresa, config);
        enviarSocket(ip, puerto, payload);
    }

    private void enviarSocket(String ip, int puerto, byte[] data) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, puerto), 3000);
            socket.setSoTimeout(5000);
            try (OutputStream out = socket.getOutputStream()) {
                out.write(data);
                out.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al comunicarse con la impresora térmica (" + ip + ":" + puerto + "): " + e.getMessage(), e);
        }
    }

    private byte[] generarPayloadTicketPrueba(ImpresoraConfigDTO config, String ip, int puerto) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int ancho = (config.getAnchoPapelMm() != null && config.getAnchoPapelMm() == 58) ? 32 : 48;
        String lineaDiv = "-".repeat(ancho);

        baos.write(ESC_INIT);

        // Encabezado
        baos.write(ESC_ALIGN_CENTER);
        baos.write(ESC_BOLD_ON);
        baos.write(GS_SIZE_DOUBLE);
        baos.write(bytes("SPA DIANA\n"));
        baos.write(GS_SIZE_NORMAL);
        baos.write(ESC_BOLD_OFF);

        baos.write(bytes("SISTEMA DE GESTION DE SALON\n"));
        baos.write(bytes(lineaDiv + "\n"));

        baos.write(ESC_BOLD_ON);
        baos.write(bytes("TICKET DE PRUEBA DE RED\n"));
        baos.write(ESC_BOLD_OFF);
        baos.write(bytes("IMPRESORA TERMICA ESC/POS\n"));
        baos.write(bytes(lineaDiv + "\n"));

        baos.write(ESC_ALIGN_LEFT);
        baos.write(bytes("Modelo:   Xprinter XP-E260L (80mm)\n"));
        baos.write(bytes("IP:       " + ip + "\n"));
        baos.write(bytes("Puerto:   " + puerto + "\n"));
        baos.write(bytes("Fecha:    " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\n"));
        baos.write(bytes("Estado:   CONEXION EXITOSA\n"));
        baos.write(bytes(lineaDiv + "\n"));

        baos.write(ESC_ALIGN_CENTER);
        baos.write(bytes("La impresora esta lista para emitir\n"));
        baos.write(bytes("tickets de venta automaticamente.\n"));
        baos.write(bytes(lineaDiv + "\n\n\n\n"));

        // Corte y gaveta si están habilitados
        if (Boolean.TRUE.equals(config.getAbrirGaveta())) {
            baos.write(ESC_DRAWER_PULSE);
        }
        if (Boolean.TRUE.equals(config.getAutoCorte())) {
            baos.write(GS_CUT_PARTIAL);
        }

        return baos.toByteArray();
    }

    private byte[] generarPayloadTicketVenta(Ticket ticket, Empresa empresa, ImpresoraConfigDTO config) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int ancho = (config.getAnchoPapelMm() != null && config.getAnchoPapelMm() == 58) ? 32 : 48;
            String lineaDiv = "-".repeat(ancho);

            baos.write(ESC_INIT);

            // 1. Datos del Negocio
            baos.write(ESC_ALIGN_CENTER);
            baos.write(ESC_BOLD_ON);
            baos.write(GS_SIZE_DOUBLE);
            String nombreComercial = empresa != null && empresa.getNombreComercial() != null ? empresa.getNombreComercial() : "SPA DIANA";
            baos.write(bytes(limpiarTexto(nombreComercial) + "\n"));
            baos.write(GS_SIZE_NORMAL);
            baos.write(ESC_BOLD_OFF);

            if (empresa != null) {
                if (empresa.getRazonSocial() != null && !empresa.getRazonSocial().isBlank()) {
                    baos.write(bytes(limpiarTexto(empresa.getRazonSocial()) + "\n"));
                }
                if (empresa.getRuc() != null && !empresa.getRuc().isBlank()) {
                    baos.write(bytes("RUC: " + empresa.getRuc() + "\n"));
                }
                if (empresa.getDireccion() != null && !empresa.getDireccion().isBlank()) {
                    baos.write(bytes(limpiarTexto(empresa.getDireccion()) + "\n"));
                }
                if (empresa.getTelefono() != null && !empresa.getTelefono().isBlank()) {
                    baos.write(bytes("Tel: " + empresa.getTelefono() + "\n"));
                }
            }

            baos.write(bytes(lineaDiv + "\n"));

            // 2. Información del Ticket
            baos.write(ESC_BOLD_ON);
            String nroOrden = String.format("#%04d", ticket.getId());
            baos.write(bytes("COMPROBANTE DE PAGO - ORDEN " + nroOrden + "\n"));
            baos.write(ESC_BOLD_OFF);
            baos.write(bytes(lineaDiv + "\n"));

            baos.write(ESC_ALIGN_LEFT);
            String fechaStr = ticket.getFechaEmision() != null
                    ? ticket.getFechaEmision().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            baos.write(bytes("FECHA:        " + fechaStr + "\n"));

            if (ticket.getCliente() != null && !ticket.getCliente().getId().equals(1L)) {
                String clienteNombre = ticket.getCliente().getNombres() + " " + (ticket.getCliente().getApellidos() != null ? ticket.getCliente().getApellidos() : "");
                baos.write(bytes("CLIENTE:      " + limpiarTexto(clienteNombre) + "\n"));
                if (ticket.getCliente().getDni() != null && !ticket.getCliente().getDni().isBlank()) {
                    baos.write(bytes("DNI/DOC:      " + ticket.getCliente().getDni().trim() + "\n"));
                }
                if (ticket.getCliente().getTelefono() != null && !ticket.getCliente().getTelefono().isBlank()) {
                    baos.write(bytes("TEL/CEL:      " + ticket.getCliente().getTelefono().trim() + "\n"));
                }
            } else if (ticket.getNombreClienteNoRegistrado() != null && !ticket.getNombreClienteNoRegistrado().isBlank()) {
                baos.write(bytes("CLIENTE:      " + limpiarTexto(ticket.getNombreClienteNoRegistrado()) + "\n"));
            } else {
                baos.write(bytes("CLIENTE:      Publico General\n"));
            }

            // Especialistas asignados en cabecera
            List<String> especialistas = new ArrayList<>();
            if (ticket.getDetalles() != null) {
                for (TicketDetalle d : ticket.getDetalles()) {
                    if (d.getEmpleado() != null) {
                        String empNombre = limpiarTexto(d.getEmpleado().getNombres() + 
                                (d.getEmpleado().getApellidos() != null ? " " + d.getEmpleado().getApellidos() : "")).trim();
                        if (!empNombre.isEmpty() && !especialistas.contains(empNombre)) {
                            especialistas.add(empNombre);
                        }
                    }
                }
            }

            if (!especialistas.isEmpty()) {
                baos.write(ESC_BOLD_ON);
                if (especialistas.size() == 1) {
                    baos.write(bytes("ESPECIALISTA: " + especialistas.get(0) + "\n"));
                } else {
                    baos.write(bytes("ESPECIALISTAS:" + String.join(", ", especialistas) + "\n"));
                }
                baos.write(ESC_BOLD_OFF);
            }

            baos.write(bytes("METODO PAGO:  " + (ticket.getMetodoPago() != null ? ticket.getMetodoPago().name() : "EFECTIVO") + "\n"));
            baos.write(bytes(lineaDiv + "\n"));

            // 3. Detalle de Items
            if (ancho == 48) {
                baos.write(bytes(padRight("CANT", 5) + padRight("DESCRIPCION", 33) + padLeft("TOTAL", 10) + "\n"));
            } else {
                baos.write(bytes(padRight("CT", 3) + padRight("DESCRIPCION", 20) + padLeft("TOT", 9) + "\n"));
            }
            baos.write(bytes(lineaDiv + "\n"));

            BigDecimal subtotalCalculado = BigDecimal.ZERO;
            if (ticket.getDetalles() != null) {
                for (TicketDetalle d : ticket.getDetalles()) {
                    int cant = d.getCantidad() != null ? d.getCantidad() : 1;
                    String desc = "";
                    if (d.getServicio() != null) {
                        desc = d.getServicio().getNombre();
                    } else if (d.getProducto() != null) {
                        desc = d.getProducto().getNombre();
                    } else {
                        desc = "Item";
                    }
                    desc = limpiarTexto(desc);

                    BigDecimal subt = d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO;
                    subtotalCalculado = subtotalCalculado.add(subt);

                    String cantStr = String.valueOf(cant);
                    String subtStr = String.format("%.2f", subt);

                    String catNombre = "";
                    if (d.getServicio() != null && d.getServicio().getCategoria() != null) {
                        catNombre = limpiarTexto(d.getServicio().getCategoria().getNombre()).trim();
                    } else if (d.getProducto() != null && d.getProducto().getCategoria() != null) {
                        catNombre = limpiarTexto(d.getProducto().getCategoria().getNombre()).trim();
                    }

                    String empNombre = (d.getEmpleado() != null)
                            ? limpiarTexto(d.getEmpleado().getNombres() + (d.getEmpleado().getApellidos() != null ? " " + d.getEmpleado().getApellidos() : "")).trim()
                            : "";

                    if (ancho == 48) {
                        String descLine = padRight(desc, 33);
                        baos.write(bytes(padRight(cantStr, 5) + descLine + padLeft(subtStr, 10) + "\n"));

                        if (!catNombre.isEmpty() && !empNombre.isEmpty()) {
                            baos.write(bytes("     Cat: " + catNombre + "\n"));
                            baos.write(ESC_BOLD_ON);
                            baos.write(bytes("     >> Especialista: " + empNombre + "\n"));
                            baos.write(ESC_BOLD_OFF);
                        } else if (!catNombre.isEmpty()) {
                            baos.write(bytes("     Cat: " + catNombre + "\n"));
                        } else if (!empNombre.isEmpty()) {
                            baos.write(ESC_BOLD_ON);
                            baos.write(bytes("     >> Especialista: " + empNombre + "\n"));
                            baos.write(ESC_BOLD_OFF);
                        }
                    } else {
                        String descLine = padRight(desc, 20);
                        baos.write(bytes(padRight(cantStr, 3) + descLine + padLeft(subtStr, 9) + "\n"));

                        if (!catNombre.isEmpty()) {
                            baos.write(bytes("  Cat: " + catNombre + "\n"));
                        }
                        if (!empNombre.isEmpty()) {
                            baos.write(ESC_BOLD_ON);
                            baos.write(bytes("  >> Esp: " + empNombre + "\n"));
                            baos.write(ESC_BOLD_OFF);
                        }
                    }
                }
            }

            baos.write(bytes(lineaDiv + "\n"));

            // 4. Totales
            baos.write(ESC_ALIGN_RIGHT);
            BigDecimal totalFinal = ticket.getTotal() != null ? ticket.getTotal() : subtotalCalculado;
            BigDecimal descuento = subtotalCalculado.subtract(totalFinal);
            if (descuento.compareTo(BigDecimal.ZERO) > 0) {
                baos.write(bytes("SUBTOTAL: S/ " + String.format("%.2f", subtotalCalculado) + "\n"));
                baos.write(bytes("DESCUENTO: -S/ " + String.format("%.2f", descuento) + "\n"));
            }

            baos.write(ESC_BOLD_ON);
            baos.write(GS_SIZE_DOUBLE);
            baos.write(bytes("TOTAL: S/ " + String.format("%.2f", totalFinal) + "\n"));
            baos.write(GS_SIZE_NORMAL);
            baos.write(ESC_BOLD_OFF);

            // 5. Fidelización y Sellos de la Clienta
            String asteriscoDiv = "*".repeat(ancho);
            if (ticket.getCliente() != null && !ticket.getCliente().getId().equals(1L)) {
                try {
                    // 1. Mostrar premio de SERVICIO GRATIS ÚNICAMENTE en el ticket donde se completó
                    List<ClienteCartilla> cartillasCompletadasEnEsteTicket = clienteCartillaRepository.findByTicketCompletadoId(ticket.getId());
                    if (cartillasCompletadasEnEsteTicket != null && !cartillasCompletadasEnEsteTicket.isEmpty()) {
                        for (ClienteCartilla ccComp : cartillasCompletadasEnEsteTicket) {
                            if (ccComp.getCartillaFidelizacion() == null) continue;
                            String nombreCartilla = limpiarTexto(ccComp.getCartillaFidelizacion().getNombre());
                            int meta = (ccComp.getCartillaFidelizacion().getMetaSellos() != null && ccComp.getCartillaFidelizacion().getMetaSellos() > 0)
                                    ? ccComp.getCartillaFidelizacion().getMetaSellos() : 5;

                            baos.write(ESC_ALIGN_CENTER);
                            baos.write(bytes(asteriscoDiv + "\n"));
                            baos.write(ESC_BOLD_ON);
                            baos.write(bytes("*** FELICIDADES: CARTILLA COMPLETADA ***\n"));
                            baos.write(ESC_BOLD_OFF);
                            baos.write(bytes(asteriscoDiv + "\n"));

                            baos.write(ESC_ALIGN_LEFT);
                            baos.write(bytes("Cartilla: " + nombreCartilla + "\n"));
                            baos.write(bytes("Meta:     [" + barraProgreso(meta, meta) + "] (" + meta + "/" + meta + " sellos)\n"));
                            baos.write(ESC_ALIGN_CENTER);
                            baos.write(ESC_BOLD_ON);
                            baos.write(bytes(">> TIENES UN SERVICIO GRATIS! <<\n"));
                            baos.write(ESC_BOLD_OFF);
                            baos.write(bytes("Presenta este ticket o tu DNI en tu\n"));
                            baos.write(bytes("proxima visita para canjear tu premio.\n"));
                            baos.write(bytes(asteriscoDiv + "\n"));
                        }
                    }

                    // 2. Mostrar progreso de las cartillas activas (en curso) que aún no están completadas
                    List<ClienteCartilla> cartillasActivas = clienteCartillaRepository.findActivasByClienteId(ticket.getCliente().getId());
                    if (cartillasActivas != null && !cartillasActivas.isEmpty()) {
                        int mostradas = 0;
                        for (ClienteCartilla cc : cartillasActivas) {
                            if (mostradas >= 2) break;
                            if (cc.getCartillaFidelizacion() == null) continue;
                            if (Boolean.TRUE.equals(cc.getCompletada())) continue; // Si ya se completó, no se muestra aquí

                            String nombreCartilla = limpiarTexto(cc.getCartillaFidelizacion().getNombre());
                            int actual = cc.getSellosActuales() != null ? cc.getSellosActuales() : 0;
                            int meta = (cc.getCartillaFidelizacion().getMetaSellos() != null && cc.getCartillaFidelizacion().getMetaSellos() > 0)
                                    ? cc.getCartillaFidelizacion().getMetaSellos() : 5;
                            int faltan = Math.max(0, meta - actual);

                            baos.write(ESC_ALIGN_CENTER);
                            baos.write(bytes(lineaDiv + "\n"));
                            baos.write(ESC_BOLD_ON);
                            baos.write(bytes("CLUB DE FIDELIZACION & SELLOS\n"));
                            baos.write(ESC_BOLD_OFF);
                            baos.write(bytes(lineaDiv + "\n"));

                            baos.write(ESC_ALIGN_LEFT);
                            baos.write(bytes("Cartilla: " + nombreCartilla + "\n"));
                            baos.write(bytes("Progreso: [" + barraProgreso(actual, meta) + "] (" + actual + "/" + meta + " sellos)\n"));
                            baos.write(ESC_BOLD_ON);
                            baos.write(bytes("Te faltan " + faltan + " sello(s) para tu premio!\n"));
                            baos.write(ESC_BOLD_OFF);
                            baos.write(bytes(lineaDiv + "\n"));
                            mostradas++;
                        }
                    }
                } catch (Exception ex) {
                    log.warn("No se pudo obtener información de fidelización para ticket {}: {}", ticket.getId(), ex.getMessage());
                }
            }

            // 6. Pie de Página
            baos.write(ESC_ALIGN_CENTER);
            baos.write(bytes(lineaDiv + "\n"));
            String pie = config.getMensajePie() != null && !config.getMensajePie().isBlank()
                    ? config.getMensajePie()
                    : "Gracias por su preferencia!";
            baos.write(bytes(limpiarTexto(pie) + "\n"));
            baos.write(bytes(lineaDiv + "\n\n\n\n"));

            // Corte y gaveta si están habilitados
            if (Boolean.TRUE.equals(config.getAbrirGaveta())) {
                baos.write(ESC_DRAWER_PULSE);
            }
            if (Boolean.TRUE.equals(config.getAutoCorte())) {
                baos.write(GS_CUT_PARTIAL);
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al estructurar el ticket térmico: " + e.getMessage(), e);
        }
    }

    private String normalizarIp(String ip) {
        if (ip == null) return "192.168.123.100";
        // Si el usuario accidentalmente escribe doble punto "192.168..123.100", lo corrige
        String corregida = ip.replaceAll("\\.{2,}", ".").trim();
        return corregida.isEmpty() ? "192.168.123.100" : corregida;
    }

    private String limpiarTexto(String texto) {
        if (texto == null) return "";
        // Reemplaza tildes, ñ y caracteres fuera de US-ASCII para compatibilidad con impresoras térmicas
        String normalized = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .replace("ñ", "n").replace("Ñ", "N")
                .replace("¡", "")
                .replace("¿", "");
    }

    private byte[] bytes(String str) {
        return str.getBytes(StandardCharsets.US_ASCII);
    }

    private String padRight(String s, int n) {
        if (s == null) s = "";
        if (s.length() > n) return s.substring(0, n);
        return String.format("%-" + n + "s", s);
    }

    private String padLeft(String s, int n) {
        if (s == null) s = "";
        if (s.length() > n) return s.substring(0, n);
        return String.format("%" + n + "s", s);
    }

    private String barraProgreso(int actual, int meta) {
        if (meta <= 0) meta = 5;
        int maxStars = Math.min(meta, 8);
        int filled = (int) Math.round(((double) actual / meta) * maxStars);
        filled = Math.max(0, Math.min(maxStars, filled));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filled; i++) {
            sb.append("* ");
        }
        for (int i = filled; i < maxStars; i++) {
            sb.append(". ");
        }
        return sb.toString().trim();
    }
}

