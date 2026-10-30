package pe.com.salon.salongestionapi.auth.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import pe.com.salon.salongestionapi.security.entity.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${jwt.public-key-path}")
    private String publicKeyPath;

    @Value("${jwt.expiration}")
    private Long defaultExpiration;

    private volatile PrivateKey privateKey;
    private volatile PublicKey publicKey;

    private final pe.com.salon.salongestionapi.shared.service.ConfiguracionService configuracionService;

    public JwtService(pe.com.salon.salongestionapi.shared.service.ConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
    }

    public String generateToken(Usuario user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("sexo", user.getSexo());

        claims.put("roles", user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList()));

        claims.put("permissions", user.getPermissionNames());

        Date now = new Date();

        // Get dynamic expiration from DB (in minutes) and convert to milliseconds
        Long minutesObject = configuracionService.getGlobalSessionTimeout();
        // Fallback to default if 0 or null (though service handles defaults)
        long expirationMillis = (minutesObject != null && minutesObject > 0)
                ? minutesObject * 60 * 1000
                : defaultExpiration;

        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extraer fecha de expiración del token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extraer userId del token
     */
    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    /**
     * Obtener tiempo restante en milisegundos
     */
    public Long getTimeRemaining(String token) {
        Date expiration = extractExpiration(token);
        Date now = new Date();
        long remaining = expiration.getTime() - now.getTime();
        return remaining > 0 ? remaining : 0L;
    }

    /**
     * Obtener tiempo restante en segundos
     */
    public Long getTimeRemainingInSeconds(String token) {
        return getTimeRemaining(token) / 1000;
    }

    /**
     * Verificar si el token ha expirado
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Validar token
     */
    public boolean isTokenValid(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtener tiempo de expiración configurado en milisegundos
     */
    public Long getExpirationTime() {
        Long minutes = configuracionService.getGlobalSessionTimeout();
        return (minutes != null && minutes > 0) ? minutes * 60 * 1000 : defaultExpiration;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((java.security.interfaces.RSAPublicKey) getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private PrivateKey getPrivateKey() {
        if (privateKey == null) {
            loadOrCreateKeys();
        }
        return privateKey;
    }

    private PublicKey getPublicKey() {
        if (publicKey == null) {
            loadOrCreateKeys();
        }
        return publicKey;
    }

    /**
     * Carga el par de llaves RSA una sola vez. Si se configuran rutas de archivo (por defecto ./keys)
     * y no existen, se genera un par nuevo y se guarda ahí, para que las llaves nunca viajen en el repositorio.
     */
    private synchronized void loadOrCreateKeys() {
        if (privateKey != null && publicKey != null) {
            return;
        }
        try {
            if (!privateKeyPath.startsWith("classpath:")) {
                Path privatePath = Path.of(privateKeyPath.replace("file:", ""));
                Path publicPath = Path.of(publicKeyPath.replace("file:", ""));
                if (!Files.exists(privatePath) || !Files.exists(publicPath)) {
                    generateKeyFiles(privatePath, publicPath);
                }
            }

            String privateContent = loadKeyContent(privateKeyPath)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            String publicContent = loadKeyContent(publicKeyPath)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            KeyFactory kf = KeyFactory.getInstance("RSA");
            privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateContent)));
            publicKey = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicContent)));
        } catch (Exception e) {
            log.error("Could not load RSA keys", e);
            throw new RuntimeException("Could not load RSA keys", e);
        }
    }

    private void generateKeyFiles(Path privatePath, Path publicPath) throws Exception {
        log.warn("No se encontraron llaves JWT en {}. Generando un par RSA nuevo (las sesiones anteriores quedarán invalidadas).",
                privatePath.toAbsolutePath().getParent());
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        Base64.Encoder encoder = Base64.getMimeEncoder(64, "\n".getBytes());
        String privatePem = "-----BEGIN PRIVATE KEY-----\n"
                + encoder.encodeToString(pair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        String publicPem = "-----BEGIN PUBLIC KEY-----\n"
                + encoder.encodeToString(pair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";

        if (privatePath.toAbsolutePath().getParent() != null) {
            Files.createDirectories(privatePath.toAbsolutePath().getParent());
        }
        if (publicPath.toAbsolutePath().getParent() != null) {
            Files.createDirectories(publicPath.toAbsolutePath().getParent());
        }
        Files.writeString(privatePath, privatePem);
        Files.writeString(publicPath, publicPem);
    }

    private String loadKeyContent(String path) throws Exception {
        if (path.startsWith("classpath:")) {
            try (InputStream inputStream = new ClassPathResource(path.replace("classpath:", "")).getInputStream()) {
                return new String(inputStream.readAllBytes());
            }
        }
        return Files.readString(Path.of(path.replace("file:", "")));
    }

    /**
     * Generar token con información de sucursal (Modelo Híbrido)
     */
    public String generateTokenWithSucursal(Usuario user, Long sucursalId, String sucursalNombre) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("sexo", user.getSexo());
        claims.put("roles", user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList()));
        claims.put("permissions", user.getPermissionNames());

        // NUEVO: Incluir sucursal en JWT
        claims.put("sucursalId", sucursalId);
        claims.put("sucursalNombre", sucursalNombre);

        Date now = new Date();
        Long minutesObject = configuracionService.getGlobalSessionTimeout();
        long expirationMillis = (minutesObject != null && minutesObject > 0)
                ? minutesObject * 60 * 1000
                : defaultExpiration;
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * Extraer sucursalId del token
     */
    public Long extractSucursalId(String token) {
        try {
            return extractAllClaims(token).get("sucursalId", Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extraer sucursalNombre del token
     */
    public String extractSucursalNombre(String token) {
        try {
            return extractAllClaims(token).get("sucursalNombre", String.class);
        } catch (Exception e) {
            return null;
        }
    }
}
