package pe.com.salon.salongestionapi.auth.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.salon.salongestionapi.auth.service.dto.LoginRequest;
import pe.com.salon.salongestionapi.auth.service.dto.LoginResponse;
import pe.com.salon.salongestionapi.auth.service.dto.RegisterRequest;
import pe.com.salon.salongestionapi.auth.service.dto.RegisterResponse;
import pe.com.salon.salongestionapi.exception.AuthException;
import pe.com.salon.salongestionapi.exception.ValidationException;
import pe.com.salon.salongestionapi.reports.audit.service.AuditService;
import pe.com.salon.salongestionapi.roles.entity.Role;
import pe.com.salon.salongestionapi.roles.repository.RoleRepository;
import pe.com.salon.salongestionapi.security.entity.FailureReason;
import pe.com.salon.salongestionapi.security.entity.Usuario;
import pe.com.salon.salongestionapi.security.repository.UsuarioRepository;
import pe.com.salon.salongestionapi.security.service.SecurityMonitorService;
import pe.com.salon.salongestionapi.security.util.HttpUtils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final SecurityMonitorService securityMonitorService;
    private final AuditService auditService;

    public AuthService(UsuarioRepository usuarioRepository,
            RoleRepository roleRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            SecurityMonitorService securityMonitorService,
            AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.securityMonitorService = securityMonitorService;
        this.auditService = auditService;
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = HttpUtils.getClientIpAddress(httpRequest);
        String userAgent = HttpUtils.getUserAgent(httpRequest);

        log.info("Login attempt for user: {} from IP: {}", request.getUsername(), ipAddress);

        if (securityMonitorService.isIpBlocked(ipAddress)) {
            int remainingAttempts = securityMonitorService.getRemainingAttempts(ipAddress);
            var unblockTime = securityMonitorService.getUnblockTime(ipAddress);
            securityMonitorService.registerFailedAttempt(request.getUsername(), ipAddress, userAgent,
                    FailureReason.IP_BLOCKED);
            throw new AuthException("IP bloqueada temporalmente.", remainingAttempts, true, unblockTime);
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException e) {
            securityMonitorService.registerFailedAttempt(request.getUsername(), ipAddress, userAgent,
                    FailureReason.INVALID_CREDENTIALS);
            int remainingAttempts = securityMonitorService.getRemainingAttempts(ipAddress);
            String message = remainingAttempts == 0 ? "IP bloqueada." : "Credenciales inválidas.";
            throw new AuthException(message, remainingAttempts, remainingAttempts == 0, null);
        }

        Usuario user = usuarioRepository.findByLoginWithRoles(request.getUsername())
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));

        if (!user.getActive()) {
            securityMonitorService.registerFailedAttempt(request.getUsername(), ipAddress, userAgent,
                    FailureReason.ACCOUNT_BLOCKED);
            throw new AuthException("La cuenta ha sido bloqueada.");
        }

        user.setLastLogin(LocalDateTime.now());
        usuarioRepository.save(user);

        String token = jwtService.generateToken(user);

        securityMonitorService.registerSuccessfulLogin(user, ipAddress, userAgent, token);

        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        Set<String> allPermissions = new HashSet<>(user.getPermissionNames());

        String firstName = user.getEmpleado() != null ? user.getEmpleado().getNombres() : user.getUsername();
        String lastName = user.getEmpleado() != null ? user.getEmpleado().getApellidos() : "";
        Long idPersonal = user.getEmpleado() != null ? user.getEmpleado().getId() : null;
        String telefono = user.getEmpleado() != null ? user.getEmpleado().getTelefono() : "Sin Teléfono";
        String dni = user.getEmpleado() != null ? user.getEmpleado().getDni() : "Sin DNI";

        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .idPersonal(idPersonal)
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(firstName)
                .avatarUrl(user.getAvatarUrl())
                .sexo(user.getSexo())
                .telefono(telefono)
                .dni(dni)
                .lastName(lastName)
                .roles(roleNames)
                .permissions(allPermissions)
                .build();
    }

    public RegisterResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("El nombre de usuario ya existe.");
        }
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("El email ya está registrado.");
        }

        Usuario newUser = Usuario.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .estado(true)
                .roles(new HashSet<>())
                .build();

        Set<Role> assignedRoles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String roleName : request.getRoles()) {
                Role role = roleRepository.findByName(roleName.toUpperCase())
                        .orElseThrow(() -> new ValidationException("Rol no encontrado: " + roleName));
                assignedRoles.add(role);
            }
        } else {
            Role defaultRole = roleRepository.findByName("CLIENTE").orElse(null);
            if (defaultRole != null)
                assignedRoles.add(defaultRole);
        }

        newUser.setRoles(assignedRoles);
        Usuario savedUser = usuarioRepository.save(newUser);

        Set<String> roleNames = savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet());

        return RegisterResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .firstName(savedUser.getUsername())
                .lastName("")
                .roles(roleNames)
                .message("Usuario registrado exitosamente")
                .build();
    }

    public void logout(String token, HttpServletRequest httpRequest) {
        // Lógica de logout delegada al JwtService o Monitor
    }

    public pe.com.salon.salongestionapi.auth.service.dto.TokenInfoResponse getTokenInfo(String token) {
        return pe.com.salon.salongestionapi.auth.service.dto.TokenInfoResponse.builder()
                .timeRemainingMs(jwtService.getTimeRemaining(token))
                .timeRemainingSeconds(jwtService.getTimeRemainingInSeconds(token))
                .expirationTimeMs(jwtService.getExpirationTime())
                .isExpired(jwtService.isTokenExpired(token))
                .build();
    }

    public pe.com.salon.salongestionapi.auth.service.dto.TokenInfoResponse refreshToken(String oldToken) {
        String username = jwtService.extractUsername(oldToken);
        Usuario user = usuarioRepository.findByLoginWithRoles(username)
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));

        if (!user.getActive())
            throw new AuthException("Cuenta inactiva");

        String newToken = jwtService.generateToken(user);

        return pe.com.salon.salongestionapi.auth.service.dto.TokenInfoResponse.builder()
                .token(newToken)
                .timeRemainingMs(jwtService.getTimeRemaining(newToken))
                .timeRemainingSeconds(jwtService.getTimeRemainingInSeconds(newToken))
                .expirationTimeMs(jwtService.getExpirationTime())
                .isExpired(false)
                .build();
    }

    public void updateAvatar(pe.com.salon.salongestionapi.auth.service.dto.AvatarUpdateRequest request, String token) {
        String username = jwtService.extractUsername(token);
        Usuario user = usuarioRepository.findByLoginWithRoles(username)
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));
        
        user.setAvatarUrl(request.getAvatarUrl());
        usuarioRepository.save(user);
    }
}
