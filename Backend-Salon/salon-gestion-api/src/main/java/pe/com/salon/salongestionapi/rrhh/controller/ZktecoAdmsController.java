package pe.com.salon.salongestionapi.rrhh.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.rrhh.service.biometrico.BiometricoService;

/**
 * Receptor nativo del Protocolo PUSH (ADMS / iClock) de ZKTeco para el SenseFace 3A (T&A).
 * El dispositivo se conecta por HTTP a /iclock/cdata y /iclock/getrequest para enviar
 * marcaciones en tiempo real (ATTLOG) y mantener el estado en linea.
 */
@RestController
@RequestMapping("/iclock")
@RequiredArgsConstructor
@Slf4j
public class ZktecoAdmsController {

    private final BiometricoService biometricoService;

    /**
     * Handshake inicial cuando el ZKTeco SenseFace 3A se conecta al servidor ADMS.
     */
    @GetMapping(value = "/cdata", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handshake(
            @RequestParam(value = "SN", required = false, defaultValue = "ZKTECO") String sn,
            @RequestParam(value = "options", required = false) String options,
            HttpServletRequest request) {
        String ipRemota = request.getRemoteAddr();
        biometricoService.registrarLatidoPush(sn, ipRemota);
        log.info(">>> [ZKTECO ADMS] Handshake exitoso con SenseFace 3A [SN={}, IP={}]", sn, ipRemota);

        String respuesta = "GET OPTION FROM: " + sn + "\n" +
                "ATTLOGStamp=None\n" +
                "OPERLOGStamp=9999\n" +
                "ATTPHOTOStamp=None\n" +
                "ErrorDelay=30\n" +
                "Delay=10\n" +
                "TransTimes=00:00;14:05\n" +
                "TransInterval=1\n" +
                "TransFlag=TransData AttLog\n" +
                "TimeZone=-5\n" +
                "Realtime=1\n" +
                "Encrypt=0\n";
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Recepcion en tiempo real de marcaciones de entrada/salida enviadas por el SenseFace 3A.
     */
    @PostMapping(value = "/cdata", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> recibirDatosPush(
            @RequestParam(value = "SN", required = false, defaultValue = "ZKTECO") String sn,
            @RequestParam(value = "table", required = false, defaultValue = "ATTLOG") String table,
            @RequestBody(required = false) String body,
            HttpServletRequest request) {
        String ipRemota = request.getRemoteAddr();
        biometricoService.registrarLatidoPush(sn, ipRemota);

        log.info(">>> [ZKTECO ADMS] Payload recibido de SN={} (IP={}, tabla={}):\n{}", sn, ipRemota, table, body != null ? body.trim() : "<vacio>");

        int procesados = 0;
        if (body != null && !body.isBlank() && ("ATTLOG".equalsIgnoreCase(table) || "rtlog".equalsIgnoreCase(table))) {
            procesados = biometricoService.procesarPayloadPushAttLog(sn, ipRemota, body);
        } else if (body != null && !body.isBlank()) {
            String[] lineas = body.split("\\r?\\n");
            procesados = lineas.length;
        }
        return ResponseEntity.ok("OK: " + Math.max(1, procesados));
    }

    /**
     * Latido (Heartbeat) periodico del SenseFace 3A para consultar comandos pendientes.
     */
    @GetMapping(value = "/getrequest", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getRequest(
            @RequestParam(value = "SN", required = false, defaultValue = "ZKTECO") String sn,
            HttpServletRequest request) {
        biometricoService.registrarLatidoPush(sn, request.getRemoteAddr());
        return ResponseEntity.ok("OK");
    }

    /**
     * Confirmacion de ejecucion de comandos del dispositivo.
     */
    @PostMapping(value = "/devicecmd", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deviceCmd(
            @RequestParam(value = "SN", required = false, defaultValue = "ZKTECO") String sn,
            @RequestBody(required = false) String body,
            HttpServletRequest request) {
        biometricoService.registrarLatidoPush(sn, request.getRemoteAddr());
        return ResponseEntity.ok("OK");
    }
}
