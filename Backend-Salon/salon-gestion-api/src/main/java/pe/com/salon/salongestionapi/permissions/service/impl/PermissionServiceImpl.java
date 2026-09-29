package pe.com.salon.salongestionapi.permissions.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.salon.salongestionapi.exception.BadRequestException;
import pe.com.salon.salongestionapi.exception.ResourceNotFoundException;
import pe.com.salon.salongestionapi.exception.ValidationException;
import pe.com.salon.salongestionapi.permissions.dto.PermissionRequest;
import pe.com.salon.salongestionapi.permissions.dto.PermissionResponse;
import pe.com.salon.salongestionapi.permissions.entity.Permission;
import pe.com.salon.salongestionapi.permissions.repository.PermissionRepository;
import pe.com.salon.salongestionapi.permissions.service.PermissionService;
import pe.com.salon.salongestionapi.shared.PageResponse;
import pe.com.salon.salongestionapi.reports.audit.service.AuditService;
import pe.com.salon.salongestionapi.security.util.HttpUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final AuditService auditService;
    private final jakarta.servlet.http.HttpServletRequest httpRequest;

    // ========================================
    // CONSTANTES
    // ========================================
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MIN_DESCRIPTION_LENGTH = 5;
    private static final int MAX_DESCRIPTION_LENGTH = 255;
    private static final int MIN_MODULE_LENGTH = 2;
    private static final int MAX_MODULE_LENGTH = 50;
    private static final int MIN_SEARCH_LENGTH = 2;

    @Override
    @Transactional
    public PermissionResponse createPermission(PermissionRequest request) {
        log.info("Creating permission: {}", request.getName());

        // ========================================
        // VALIDACIONES
        // ========================================

        // Validar nombre
        validatePermissionName(request.getName());

        // Validar nombre único
        String normalizedName = request.getName().trim().toUpperCase();
        if (permissionRepository.existsByName(normalizedName)) {
            throw new ValidationException("Ya existe un permiso con el nombre '" + request.getName() + "'");
        }

        // Validar descripción
        validateDescription(request.getDescription());

        // Validar módulo
        validateModule(request.getModule());

        // Validar active
        if (request.getActive() == null) {
            request.setActive(true); // Default
        }

        // ========================================
        // CREACIÓN
        // ========================================

        Permission permission = Permission.builder()
                .name(normalizedName)
                .description(request.getDescription().trim())
                .module(request.getModule().trim().toUpperCase())
                .active(request.getActive())
                .build();

        Permission saved = permissionRepository.save(permission);
        log.info("Permission created successfully: {} - Module: {}", saved.getName(), saved.getModule());

        // Auditoría
        logAuditAction(
                "PERMISSION_CREATE",
                "Permiso creado: " + saved.getName() + " (Módulo: " + saved.getModule() + ")",
                saved.getId(),
                "/api/permissions",
                "POST");

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public PermissionResponse updatePermission(Long id, PermissionRequest request) {
        log.info("Updating permission with ID: {}", id);

        // Validar ID
        if (id == null || id <= 0) {
            throw new BadRequestException("El ID del permiso debe ser un número positivo");
        }

        // Validar que existe
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));

        // Validar nombre
        validatePermissionName(request.getName());

        // Validar nombre único (si cambió)
        String normalizedName = request.getName().trim().toUpperCase();
        if (!permission.getName().equals(normalizedName) && permissionRepository.existsByName(normalizedName)) {
            throw new ValidationException("Ya existe un permiso con el nombre '" + request.getName() + "'");
        }

        // Validar descripción
        validateDescription(request.getDescription());

        // Validar módulo
        validateModule(request.getModule());

        // Validar active
        if (request.getActive() == null) {
            throw new BadRequestException("El estado activo es obligatorio");
        }

        // ========================================
        // ACTUALIZACIÓN
        // ========================================

        permission.setName(normalizedName);
        permission.setDescription(request.getDescription().trim());
        permission.setModule(request.getModule().trim().toUpperCase());
        permission.setActive(request.getActive());

        Permission updated = permissionRepository.save(permission);
        log.info("Permission updated successfully: {}", updated.getName());

        // Auditoría
        logAuditAction(
                "PERMISSION_UPDATE",
                "Permiso actualizado: " + updated.getName(),
                updated.getId(),
                "/api/permissions/" + id,
                "PUT");

        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionById(Long id) {
        log.info("Getting permission by ID: {}", id);

        // Validar ID
        if (id == null || id <= 0) {
            throw new BadRequestException("El ID del permiso debe ser un número positivo");
        }

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));

        return mapToResponse(permission);
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionByName(String name) {
        log.info("Getting permission by name: {}", name);

        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("El nombre del permiso no puede estar vacío");
        }

        if (name.trim().length() < MIN_NAME_LENGTH) {
            throw new BadRequestException("El nombre del permiso debe tener al menos " +
                    MIN_NAME_LENGTH + " caracteres");
        }

        Permission permission = permissionRepository.findByName(name.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Permiso no encontrado con nombre: " + name));

        return mapToResponse(permission);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PermissionResponse> getAllPermissions(Pageable pageable) {
        log.info("Getting all permissions with pagination - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        validatePageable(pageable);

        Page<Permission> page = permissionRepository.findAll(pageable);
        return mapToPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PermissionResponse> getActivePermissions(Pageable pageable) {
        log.info("Getting active permissions with pagination - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        validatePageable(pageable);

        Page<Permission> page = permissionRepository.findByActive(true, pageable);
        return mapToPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissionsAsList() {
        log.info("Getting all permissions as list (no pagination)");

        return permissionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getActivePermissionsAsList() {
        log.info("Getting active permissions as list (no pagination)");

        return permissionRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        log.info("Deleting permission with ID: {}", id);

        // Validar ID
        if (id == null || id <= 0) {
            throw new BadRequestException("El ID del permiso debe ser un número positivo");
        }

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));

        // Validar que no esté ya inactivo
        if (!permission.getActive()) {
            throw new ValidationException("El permiso ya está desactivado");
        }

        // Soft delete
        permission.setActive(false);
        permissionRepository.save(permission);

        log.info("Permission soft deleted successfully: {}", permission.getName());

        // Auditoría
        logAuditAction(
                "PERMISSION_DELETE",
                "Permiso eliminado (soft delete): " + permission.getName() + " (ID: " + id + ")",
                id,
                "/api/permissions/" + id,
                "DELETE");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PermissionResponse> getPermissionsByModule(String module, Pageable pageable) {
        log.info("Getting permissions by module: {} with pagination", module);

        // Validar módulo
        validateModuleQuery(module);
        validatePageable(pageable);

        Page<Permission> page = permissionRepository.findByModule(module.trim().toUpperCase(), pageable);
        return mapToPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissionsByModuleAsList(String module) {
        log.info("Getting permissions by module: {} as list", module);

        // Validar módulo
        validateModuleQuery(module);

        return permissionRepository.findByModuleOrderByName(module.trim().toUpperCase()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PermissionResponse> getActivePermissionsByModule(String module, Pageable pageable) {
        log.info("Getting active permissions by module: {} with pagination", module);

        // Validar módulo
        validateModuleQuery(module);
        validatePageable(pageable);

        Page<Permission> page = permissionRepository.findByModuleAndActive(
                module.trim().toUpperCase(), true, pageable);
        return mapToPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getActivePermissionsByModuleAsList(String module) {
        log.info("Getting active permissions by module: {} as list", module);

        // Validar módulo
        validateModuleQuery(module);

        return permissionRepository.findByModuleAndActive(module.trim().toUpperCase(), true).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllModules() {
        log.info("Getting all distinct modules");
        return permissionRepository.findDistinctModules();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PermissionResponse> searchPermissions(String search, String target, Pageable pageable) {
        log.info("Searching permissions with term: {} in target: {} with pagination", search, target);
        if (search == null || search.trim().isEmpty()) {
            throw new BadRequestException("El término de búsqueda no puede estar vacío");
        }
        if (search.trim().length() < MIN_SEARCH_LENGTH) {
            throw new BadRequestException(
                    "El término de búsqueda debe tener al menos " + MIN_SEARCH_LENGTH + " caracteres");
        }
        String searchTarget = (target == null || target.trim().isEmpty()) ? "all" : target.trim().toLowerCase();
        validatePageable(pageable);
        Page<Permission> page = permissionRepository.searchPermissionsAdvanced(search.trim(), searchTarget, pageable);
        return mapToPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PermissionResponse> searchByFilter(String query, String type, Pageable pageable) {
        Page<Permission> page;

        switch (type.toUpperCase()) {
            case "NOMBRE":
            case "CODIGO":
                page = permissionRepository.searchPermissionsAdvanced(query, "name", pageable);
                break;
            default: // "ALL"
                page = permissionRepository.searchPermissionsAdvanced(query, "all", pageable);
                break;
        }

        return mapToPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> searchPermissionsAsList(String search) {
        log.info("Searching permissions with term: {} as list", search);

        // Validar término de búsqueda
        if (search == null || search.trim().isEmpty()) {
            throw new BadRequestException("El término de búsqueda no puede estar vacío");
        }

        if (search.trim().length() < MIN_SEARCH_LENGTH) {
            throw new BadRequestException("El término de búsqueda debe tener al menos " +
                    MIN_SEARCH_LENGTH + " caracteres");
        }

        return permissionRepository.searchPermissions(search.trim()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        // Validar nombre
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        return permissionRepository.existsByName(name.trim().toUpperCase());
    }

    private void logAuditAction(String action, String description, Long resourceId,
            String endpoint, String method) {
        try {
            // auditService.logAction(...) temporalmente deshabilitado por conflicto de firma
        } catch (Exception e) {
            log.error("Error logging audit for action: {}", action, e);
        }
    }

    private void validatePermissionName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("El nombre del permiso es obligatorio");
        }

        String trimmedName = name.trim();

        if (trimmedName.length() < MIN_NAME_LENGTH) {
            throw new BadRequestException("El nombre del permiso debe tener al menos " +
                    MIN_NAME_LENGTH + " caracteres");
        }

        if (trimmedName.length() > MAX_NAME_LENGTH) {
            throw new BadRequestException("El nombre del permiso no puede superar los " +
                    MAX_NAME_LENGTH + " caracteres");
        }

        // Validar que solo contenga letras mayúsculas, números y guiones bajos
        if (!trimmedName.matches("^[A-Z0-9_]+$")) {
            throw new BadRequestException(
                    "El nombre del permiso solo puede contener letras mayúsculas, números y guiones bajos. " +
                            "Ejemplo: ORDEN_CREAR, USUARIO_LEER");
        }
    }

    private void validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new BadRequestException("La descripción del permiso es obligatoria");
        }

        String trimmedDescription = description.trim();

        if (trimmedDescription.length() < MIN_DESCRIPTION_LENGTH) {
            throw new BadRequestException("La descripción debe tener al menos " +
                    MIN_DESCRIPTION_LENGTH + " caracteres");
        }

        if (trimmedDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BadRequestException("La descripción no puede superar los " +
                    MAX_DESCRIPTION_LENGTH + " caracteres");
        }
    }

    private void validateModule(String module) {
        if (module == null || module.trim().isEmpty()) {
            throw new BadRequestException("El módulo es obligatorio");
        }

        String trimmedModule = module.trim();

        if (trimmedModule.length() < MIN_MODULE_LENGTH) {
            throw new BadRequestException("El módulo debe tener al menos " +
                    MIN_MODULE_LENGTH + " caracteres");
        }

        if (trimmedModule.length() > MAX_MODULE_LENGTH) {
            throw new BadRequestException("El módulo no puede superar los " +
                    MAX_MODULE_LENGTH + " caracteres");
        }

        // Validar que solo contenga letras mayúsculas, números y guiones bajos
        if (!trimmedModule.matches("^[A-Z0-9_]+$")) {
            throw new BadRequestException("El módulo solo puede contener letras mayúsculas, números y guiones bajos. " +
                    "Ejemplo: ORDENES, USUARIOS, LAB_ORDEN");
        }
    }

    private void validateModuleQuery(String module) {
        if (module == null || module.trim().isEmpty()) {
            throw new BadRequestException("El nombre del módulo no puede estar vacío");
        }

        if (module.trim().length() < MIN_MODULE_LENGTH) {
            throw new BadRequestException("El nombre del módulo debe tener al menos " +
                    MIN_MODULE_LENGTH + " caracteres");
        }
    }

    private void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new BadRequestException("Los parámetros de paginación son requeridos");
        }

        if (pageable.getPageSize() > 100) {
            throw new BadRequestException("El tamaño de página no puede ser mayor a 100");
        }

        if (pageable.getPageSize() <= 0) {
            throw new BadRequestException("El tamaño de página debe ser mayor a 0");
        }
    }

    private PageResponse<PermissionResponse> mapToPageResponse(Page<Permission> page) {
        List<PermissionResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<PermissionResponse>builder()
                .content(content)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .pageSize(page.getSize())
                .pageNumber(page.getNumber())
                .isLast(page.isLast())
                .build();
    }

    private PermissionResponse mapToResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .module(permission.getModule())
                .active(permission.getActive())
                .createdAt(permission.getCreatedAt())
                .build();
    }

}
