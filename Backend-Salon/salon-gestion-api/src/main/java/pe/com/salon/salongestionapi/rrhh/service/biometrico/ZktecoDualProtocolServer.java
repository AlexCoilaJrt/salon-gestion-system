package pe.com.salon.salongestionapi.rrhh.service.biometrico;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Puente Dual HTTP + HTTPS (TLS) para el terminal ZKTeco SenseFace 3A.
 * Si el SenseFace 3A envía tráfico cifrado TLS (byte inicial 0x16) al puerto 8080 de la IP LAN,
 * este componente desencripta automáticamente el paquete TLS y lo entrega en HTTP plano a Tomcat (127.0.0.1:8080),
 * evitando el error 'Invalid character found in method name [0x160x030x01...]'.
 * Si el SenseFace 3A envía HTTP plano, lo pasa directamente sin modificarlo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ZktecoDualProtocolServer {

    private final BiometricoService biometricoService;

    @Value("${server.port:8080}")
    private int serverPort;

    private final List<ServerSocket> serverSockets = new ArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean running = true;
    private SSLSocketFactory sslSocketFactory;

    @PostConstruct
    public void iniciar() {
        try {
            // Permitir TLS 1.0, 1.1, 1.2 y 1.3 para compatibilidad total con firmware ZKTeco
            Security.setProperty("jdk.tls.disabledAlgorithms", "");
            this.sslSocketFactory = crearSslSocketFactory();

            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()) {
                NetworkInterface net = nets.nextElement();
                if (!net.isUp() || net.isLoopback()) continue;
                Enumeration<InetAddress> addrs = net.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        iniciarListenerEnDireccion(addr, serverPort);
                    }
                }
            }
            // También abrir 8443 en todas las interfaces como puerto HTTPS alternativo
            iniciarListenerEnDireccion(InetAddress.getByName("0.0.0.0"), 8443);
        } catch (Exception e) {
            log.warn("Aviso al iniciar puente dual HTTP/HTTPS para ZKTeco: {}", e.getMessage());
        }
    }

    private void iniciarListenerEnDireccion(InetAddress bindAddr, int port) {
        try {
            ServerSocket ss = new ServerSocket();
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(bindAddr, port), 50);
            serverSockets.add(ss);
            log.info(">>> [ZKTECO DUAL HTTP/HTTPS] Escuchando en {}:{} (soporta HTTP y HTTPS/TLS automatico)", bindAddr.getHostAddress(), port);

            executor.submit(() -> {
                while (running && !ss.isClosed()) {
                    try {
                        Socket client = ss.accept();
                        executor.submit(() -> manejarConexion(client));
                    } catch (IOException ignored) {
                    }
                }
            });
        } catch (IOException e) {
            log.debug("No se pudo enlazar puente dual en {}:{} ({})", bindAddr.getHostAddress(), port, e.getMessage());
        }
    }

    private void manejarConexion(Socket clientSocket) {
        String remoteIp = clientSocket.getInetAddress() != null ? clientSocket.getInetAddress().getHostAddress() : "desconocida";
        try {
            clientSocket.setSoTimeout(15000);
            InputStream rawIn = clientSocket.getInputStream();
            int firstByte = rawIn.read();
            if (firstByte == -1) {
                clientSocket.close();
                return;
            }

            // Registrar que el dispositivo en la red se acaba de comunicar con nuestro servidor
            if (!"127.0.0.1".equals(remoteIp) && !"0:0:0:0:0:0:0:1".equals(remoteIp)) {
                biometricoService.registrarLatidoPush(null, remoteIp);
            }

            Socket effectiveClientSocket = clientSocket;
            InputStream clientIn;
            OutputStream clientOut;

            if (firstByte == 0x16 && sslSocketFactory != null) {
                // El primer byte es 0x16 -> Es un handshake TLS/HTTPS enviado por el ZKTeco SenseFace 3A
                log.info(">>> [ZKTECO TLS] Conexion HTTPS/TLS detectada desde {} - desencriptando automaticamente...", remoteIp);
                ByteArrayInputStream consumed = new ByteArrayInputStream(new byte[]{(byte) firstByte});
                SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(clientSocket, consumed, true);
                sslSocket.setUseClientMode(false);
                sslSocket.setEnabledProtocols(filtrarProtocolosSoportados(sslSocket.getSupportedProtocols()));
                sslSocket.startHandshake();
                effectiveClientSocket = sslSocket;
                clientIn = sslSocket.getInputStream();
                clientOut = sslSocket.getOutputStream();
            } else {
                // Es HTTP plano
                clientIn = new SequenceInputStream(new ByteArrayInputStream(new byte[]{(byte) firstByte}), rawIn);
                clientOut = clientSocket.getOutputStream();
            }

            try (Socket backendSocket = new Socket()) {
                backendSocket.connect(new InetSocketAddress("127.0.0.1", serverPort), 3000);
                backendSocket.setSoTimeout(15000);
                InputStream backendIn = backendSocket.getInputStream();
                OutputStream backendOut = backendSocket.getOutputStream();

                Thread t = new Thread(() -> {
                    try {
                         byte[] buf = new byte[8192];
                        int n;
                        while ((n = clientIn.read(buf)) != -1) {
                            backendOut.write(buf, 0, n);
                            backendOut.flush();
                        }
                    } catch (IOException ignored) {
                    }
                });
                t.setDaemon(true);
                t.start();

                byte[] buf = new byte[8192];
                int n;
                while ((n = backendIn.read(buf)) != -1) {
                    clientOut.write(buf, 0, n);
                    clientOut.flush();
                }
            }
            effectiveClientSocket.close();
        } catch (SSLHandshakeException sslEx) {
            log.warn(">>> [ZKTECO TLS] El SenseFace 3A ({}) intento HTTPS pero rechazo el certificado autofirmado ({}). Apaga la opcion 'HTTPS' en el menu ADMS del reloj y reinicialo.", remoteIp, sslEx.getMessage());
            try { clientSocket.close(); } catch (IOException ignored) {}
        } catch (Exception e) {
            try { clientSocket.close(); } catch (IOException ignored) {}
        }
    }

    private String[] filtrarProtocolosSoportados(String[] supported) {
        List<String> list = new ArrayList<>();
        for (String p : supported) {
            if (p.startsWith("TLSv1")) {
                list.add(p);
            }
        }
        return list.toArray(new String[0]);
    }

    private SSLSocketFactory crearSslSocketFactory() throws Exception {
        Path keystorePath = Paths.get("keys", "zkteco_tls.p12");
        Files.createDirectories(keystorePath.getParent());
        char[] password = "changeit".toCharArray();

        if (!Files.exists(keystorePath)) {
            String keytool = System.getProperty("java.home") + File.separator + "bin" + File.separator + "keytool";
            ProcessBuilder pb = new ProcessBuilder(
                    keytool, "-genkeypair",
                    "-alias", "zkteco",
                    "-keyalg", "RSA",
                    "-keysize", "2048",
                    "-storetype", "PKCS12",
                    "-keystore", keystorePath.toAbsolutePath().toString(),
                    "-validity", "3650",
                    "-storepass", "changeit",
                    "-keypass", "changeit",
                    "-dname", "CN=192.168.100.64, OU=Salon, O=SpaDiana, L=Lima, ST=Lima, C=PE"
            );
            Process p = pb.start();
            p.waitFor();
        }

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = Files.newInputStream(keystorePath)) {
            ks.load(is, password);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password);

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, new SecureRandom());
        return ctx.getSocketFactory();
    }

    @PreDestroy
    public void detener() {
        running = false;
        for (ServerSocket ss : serverSockets) {
            try { ss.close(); } catch (IOException ignored) {}
        }
        executor.shutdownNow();
    }
}
