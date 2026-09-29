package pe.com.salon.salongestionapi.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.com.salon.salongestionapi.auth.service.AuthService;
import pe.com.salon.salongestionapi.auth.service.dto.LoginRequest;
import pe.com.salon.salongestionapi.auth.service.dto.LoginResponse;
import pe.com.salon.salongestionapi.auth.service.dto.RegisterRequest;
import pe.com.salon.salongestionapi.auth.service.dto.RegisterResponse;
import pe.com.salon.salongestionapi.shared.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")

@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;

        @PostMapping("/login")
        public ResponseEntity<ApiResponse<LoginResponse>> login(
                        @Valid @RequestBody LoginRequest request,
                        HttpServletRequest httpRequest) {

                LoginResponse loginResponse = authService.login(request, httpRequest);

                return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                                .success(true)
                                .message("Login exitoso")
                                .data(loginResponse)
                                .build());
        }

        @PostMapping("/register")
        public ResponseEntity<ApiResponse<RegisterResponse>> register(
                        @Valid @RequestBody RegisterRequest request) {

                RegisterResponse registerResponse = authService.register(request);

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.<RegisterResponse>builder()
                                                .success(true)
                                                .message("Usuario registrado exitosamente")
                                                .data(registerResponse)
                                                .build());
        }

        @PostMapping("/logout")
        public ResponseEntity<ApiResponse<Void>> logout(
                        @RequestHeader("Authorization") String token,
                        HttpServletRequest httpRequest) {

                // Extraer el token sin el prefijo "Bearer "
                String jwtToken = token.substring(7);
                authService.logout(jwtToken, httpRequest);

                return ResponseEntity.ok(ApiResponse.<Void>builder()
                                .success(true)
                                .message("Logout exitoso")
                                .build());
        }

        /**
         * Health check
         * GET /api/v1/auth/health
         */
        @GetMapping("/health")
        public ResponseEntity<ApiResponse<String>> health() {
                return ResponseEntity.ok(ApiResponse.<String>builder()
                                .success(true)
                                .message("Auth service is running")
                                .data("OK")
                                .build());
        }

        @GetMapping("/token-info")
        public ResponseEntity<ApiResponse<pe.com.salon.salongestionapi.auth.service.dto.TokenInfoResponse>> getTokenInfo(
                        @RequestHeader("Authorization") String token) {

                String jwtToken = token.substring(7);
                var tokenInfo = authService.getTokenInfo(jwtToken);

                return ResponseEntity.ok(
                                ApiResponse.<pe.com.salon.salongestionapi.auth.service.dto.TokenInfoResponse>builder()
                                                .success(true)
                                                .message("Información del token obtenida")
                                                .data(tokenInfo)
                                                .build());
        }

        @PostMapping("/refresh-token")
        public ResponseEntity<ApiResponse<pe.com.salon.salongestionapi.auth.service.dto.TokenInfoResponse>> refreshToken(
                        @RequestHeader("Authorization") String token) {

                String jwtToken = token.substring(7);
                var tokenInfo = authService.refreshToken(jwtToken);

                return ResponseEntity.ok(
                                ApiResponse.<pe.com.salon.salongestionapi.auth.service.dto.TokenInfoResponse>builder()
                                                .success(true)
                                                .message("Token renovado exitosamente")
                                                .data(tokenInfo)
                                                .build());
        }

        @org.springframework.web.bind.annotation.PutMapping("/profile/avatar")
        public ResponseEntity<ApiResponse<Void>> updateAvatar(
                        @RequestHeader("Authorization") String token,
                        @Valid @RequestBody pe.com.salon.salongestionapi.auth.service.dto.AvatarUpdateRequest request) {
                
                String jwtToken = token.substring(7);
                authService.updateAvatar(request, jwtToken);

                return ResponseEntity.ok(ApiResponse.<Void>builder()
                                .success(true)
                                .message("Avatar actualizado exitosamente")
                                .build());
        }
}
