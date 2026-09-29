package pe.com.salon.salongestionapi.menu.controller;

import pe.com.salon.salongestionapi.exception.BadRequestException;
import pe.com.salon.salongestionapi.menu.dto.MenuItemDTO;
import pe.com.salon.salongestionapi.menu.service.MenuService;
import pe.com.salon.salongestionapi.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")

@RequiredArgsConstructor
public class MenuController {
    
    private final MenuService menuService;
    
    /**
     * Obtiene el árbol completo de menús sin filtrar por permisos
     * Usado para la gestión de accesos por rol (modal de Accesos)
     * GET /api/menu/tree?accesoId=X
     */
    @GetMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MenuItemDTO>> obtenerArbolCompleto(@RequestParam(required = false) Long accesoId) {
        List<MenuItemDTO> tree = menuService.obtenerArbolCompleto(accesoId);
        return ResponseEntity.ok(tree);
    }

    /**
     * Obtener el menú dinámico según los permisos del usuario autenticado
     * GET /api/menu/{usuarioId}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MenuItemDTO>>> obtenerMenu(
            @PathVariable Long id,
            @RequestParam(required = false) Long accesoId) {
        
        // El parámetro 'id' puede venir como usuarioId (legacy) o accesoId (moderno)
        // Por ahora, priorizaremos accesoId si viene por query param, sino usaremos 'id'
        Long finalAccesoId = (accesoId != null) ? accesoId : id;

        // Obtener información del usuario autenticado de forma robusta
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long usuarioId = null;

        if (auth != null && auth.getPrincipal() != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof pe.com.salon.salongestionapi.rrhh.entity.Empleado) {
                usuarioId = ((pe.com.salon.salongestionapi.rrhh.entity.Empleado) principal).getId();
            } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                // Si es UserDetails, el username es el login. El service lo puede resolver.
                // Pero intentaremos buscarlo en el repository para tener el ID real
            }
        }

        // Si no se encuentra usuarioId, intentamos usar el 'id' de la ruta como fallback si es razonable
        if (usuarioId == null) {
            usuarioId = id; // Fallback extremo para compatibilidad
        }
        
        List<MenuItemDTO> menu = menuService.obtenerMenuParaUsuario(usuarioId, finalAccesoId);
        
        return ResponseEntity.ok(ApiResponse.<List<MenuItemDTO>>builder()
            .success(true)
            .message("Menú obtenido exitosamente")
            .data(menu)
            .build());
    }
}
