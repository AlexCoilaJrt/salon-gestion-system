package pe.com.salon.salongestionapi.auth.service;

import java.io.InputStream;
import java.security.KeyFactory;
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
        try {
            String keyContent = loadKeyContent(privateKeyPath);
            keyContent = keyContent
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(keyContent);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            log.error("Could not load private key", e);
            throw new RuntimeException("Could not load private key", e);
        }
    }

    private PublicKey getPublicKey() {
        try {
            String keyContent = loadKeyContent(publicKeyPath);
            keyContent = keyContent
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(keyContent);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (Exception e) {
            log.error("Could not load public key", e);
            throw new RuntimeException("Could not load public key", e);
        }
    }

    private String loadKeyContent(String path) throws Exception {
        String resourcePath = path.replace("classpath:", "");
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return new String(inputStream.readAllBytes());
        }
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
