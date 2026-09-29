package pe.com.salon.salongestionapi.perfil_modulo.controller;

import pe.com.salon.salongestionapi.menu.dto.MenuItemDTO;
import pe.com.salon.salongestionapi.perfil_modulo.entity.Modulo;
import pe.com.salon.salongestionapi.perfil_modulo.entity.PerfilModulo;
import pe.com.salon.salongestionapi.perfil_modulo.service.PerfilModuloService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PerfilModuloController {

    private final PerfilModuloService perfilModuloService;

    // ================================================
    // MÓDULOS
    // ================================================

    /**
     * GET /api/modulos → lista todos los módulos del sistema
     */
    @GetMapping("/modulos")
    public ResponseEntity<List<Modulo>> getAllModulos() {
        log.info(">>> LLEGÓ LA PETICIÓN A GET /api/modulos <<<");
        return ResponseEntity.ok(perfilModuloService.getAllModulos());
    }

    // ================================================
    // PERFIL-MÓDULO
    // ================================================

    /**
     * GET /api/perfil-modulo/{roleId} → retorna los vínculos perfilModulo de un rol
     */
    @GetMapping("/perfil-modulo/{roleId}")
    public ResponseEntity<List<PerfilModulo>> getByRole(@PathVariable Long roleId) {
        log.info("📋 Obteniendo perfil_modulo para rol {}", roleId);
        return ResponseEntity.ok(perfilModuloService.getByRoleId(roleId));
    }

    /**
     * PUT /api/perfil-modulo/{roleId} → vincula un rol a uno o más módulos.
     * Retorna el primer PerfilModulo creado/existente.
     */
    @PutMapping("/perfil-modulo/{roleId}")
    public ResponseEntity<PerfilModulo> linkRoleToModulos(
            @PathVariable Long roleId,
            @RequestBody List<Map<String, Object>> modulos) {
        log.info("🔗 Vinculando rol {} con {} módulos", roleId, modulos.size());
        PerfilModulo result = perfilModuloService.linkRoleToModulos(roleId, modulos);
        if (result == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(result);
    }

    // ================================================
    // PERFIL-MÓDULO-MENU
    // ================================================

    /**
     * GET /api/perfil-modulo-menu/{perfilModuloId} → retorna los menús asignados
     */
    @GetMapping("/perfil-modulo-menu/{perfilModuloId}")
    public ResponseEntity<List<MenuItemDTO>> getMenusByPerfil(@PathVariable Long perfilModuloId) {
        log.info("📋 Obteniendo menús para perfilModuloId={}", perfilModuloId);
        return ResponseEntity.ok(perfilModuloService.getMenusByPerfilModuloId(perfilModuloId));
    }

    /**
     * PUT /api/perfil-modulo-menu/{perfilModuloId} → reemplaza todos los menús asignados
     */
    @PutMapping("/perfil-modulo-menu/{perfilModuloId}")
    public ResponseEntity<Void> saveMenusForPerfil(
            @PathVariable Long perfilModuloId,
            @RequestBody List<Map<String, Object>> menus) {
        log.info("💾 Guardando menús para perfilModuloId={}", perfilModuloId);
        perfilModuloService.saveMenusForPerfilModulo(perfilModuloId, menus);
        return ResponseEntity.ok().build();
    }
}
