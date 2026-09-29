package pe.com.salon.salongestionapi.roles.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.salon.salongestionapi.exception.BadRequestException;
import pe.com.salon.salongestionapi.exception.ResourceNotFoundException;
import pe.com.salon.salongestionapi.exception.ValidationException;
import pe.com.salon.salongestionapi.permissions.entity.Permission;
import pe.com.salon.salongestionapi.permissions.repository.PermissionRepository;
import pe.com.salon.salongestionapi.permissions.dto.PermissionResponse;
import pe.com.salon.salongestionapi.roles.dto.RoleRequest;
import pe.com.salon.salongestionapi.roles.dto.RoleResponse;
import pe.com.salon.salongestionapi.roles.entity.Role;
import pe.com.salon.salongestionapi.roles.repository.RoleRepository;
import pe.com.salon.salongestionapi.roles.service.RoleService;
import pe.com.salon.salongestionapi.shared.PageResponse;
import pe.com.salon.salongestionapi.reports.audit.service.AuditService;
import pe.com.salon.salongestionapi.security.util.HttpUtils;

import pe.com.salon.salongestionapi.menu.entity.AccesoMain;
import pe.com.salon.salongestionapi.menu.repository.AccesoMainRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AccesoMainRepository accesoMainRepository;
    private final pe.com.salon.salongestionapi.reports.audit.service.AuditService auditService;
    private final jakarta.servlet.http.HttpServletRequest httpRequest;

    // ========================================
    // CONSTANTES
    // ========================================
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final int MAX_PERMISSIONS_PER_ROLE = 100;

    // ========================================
    // CREAR ROL
    // ========================================

    @Override
    public RoleResponse createRole(RoleRequest request) {
        log.info("Creating role: {}", request.getName());

        // ========================================
        // VALIDACIONES
        // ========================================

        // Validar nombre
        validateRoleName(request.getName());

        // Validar nombre único
        String normalizedName = request.getName().trim().toUpperCase();
        if (roleRepository.existsByName(normalizedName)) {
            throw new ValidationException("Ya existe un rol con el nombre '" + request.getName() + "'");
        }

        // Validar descripción
        if (request.getDescription() != null) {
            String trimmedDescription = request.getDescription().trim();
            if (trimmedDescription.length() > MAX_DESCRIPTION_LENGTH) {
                throw new BadRequestException(
                        "La descripción no puede superar los " + MAX_DESCRIPTION_LENGTH + " caracteres");
            }
            if (trimmedDescription.isEmpty()) {
                request.setDescription(null); // Limpiar strings vacíos
            }
        }

        // Validar active
        if (request.getActive() == null) {
            request.setActive(true); // Default
        }

        // Validar y buscar AccesoMain
        // Validar y buscar AccesoMain
        if (request.getIdAcceso() == null || request.getIdAcceso() <= 0) {
            log.info("idAcceso no proporcionado, buscando acceso de Rehabilitacion por defecto...");
            AccesoMain accesoRehab = accesoMainRepository.findAll().stream()
                    .filter(a -> a.getNombre().toUpperCase().contains("REHAB"))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("El ID del acceso es obligatorio y no se encontró un portal de Rehabilitacion configurado"));
            request.setIdAcceso(accesoRehab.getId());
        }

        AccesoMain accesoMain = accesoMainRepository.findById(request.getIdAcceso())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Acceso no encontrado con ID: " + request.getIdAcceso()));

        // ========================================
        // CREACIÓN
        // ========================================

        Role role = Role.builder()
                .name(normalizedName)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .active(request.getActive())
                .acceso(accesoMain)
                .permissions(new HashSet<>())
                .build();

        // Asignar permisos si existen
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            validateAndAssignPermissions(role, request.getPermissionIds());
        }

        Role savedRole = roleRepository.save(role);
        log.info("Role created successfully: {} with {} permissions",
                savedRole.getName(), savedRole.getPermissions().size());

        // Auditoría
        logAuditAction(
                "ROLE_CREATE",
                "Rol creado: " + savedRole.getName() + " (Permisos: " + savedRole.getPermissions().size() + ")",
                savedRole.getId(),
                "/api/roles",
                "POST");

        return mapToResponse(savedRole);
    }

    // ========================================
    // ACTUALIZAR ROL
    // ========================================

    @Override
    public RoleResponse updateRole(Long id, RoleRequest request) {
        log.info("Updating role with id: {}", id);

        // Validar ID
        if (id == null || id <= 0) {
            throw new BadRequestException("El ID del rol debe ser un número positivo");
        }

        // Validar que existe
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));

        // Validar nombre
        validateRoleName(request.getName());

        // Validar nombre único (si cambió)
        String normalizedName = request.getName().trim().toUpperCase();
        if (!role.getName().equals(normalizedName) && roleRepository.existsByName(normalizedName)) {
            throw new ValidationException("Ya existe un rol con el nombre '" + request.getName() + "'");
        }

        // Validar descripción
        if (request.getDescription() != null) {
            String trimmedDescription = request.getDescription().trim();
            if (trimmedDescription.length() > MAX_DESCRIPTION_LENGTH) {
                throw new BadRequestException(
                        "La descripción no puede superar los " + MAX_DESCRIPTION_LENGTH + " caracteres");
            }
        }

        // No permitir desactivar roles con usuarios asignados
        if (request.getActive() != null && !request.getActive() && role.getActive()) {
            if (role.getUsers() != null && !role.getUsers().isEmpty()) {
                throw new ValidationException("No se puede desactivar un rol que tiene " +
                        role.getUsers().size() + " usuario(s) asignado(s)");
            }
        }

        // ========================================
        // ACTUALIZACIÓN
        // ========================================

        role.setName(normalizedName);
        role.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        if (request.getActive() != null) {
            role.setActive(request.getActive());
        }

        // Actualizar permisos si vienen en el request
        if (request.getPermissionIds() != null) {
            role.getPermissions().clear();
            if (!request.getPermissionIds().isEmpty()) {
                validateAndAssignPermissions(role, request.getPermissionIds());
            }
        }

        if (request.getIdAcceso() != null && request.getIdAcceso() > 0) {
            AccesoMain nuevoAcceso = accesoMainRepository.findById(request.getIdAcceso())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Acceso no encontrado con ID: " + request.getIdAcceso()));
            role.setAcceso(nuevoAcceso);
        }

        Role updatedRole = roleRepository.save(role);
        log.info("Role updated successfully: {} with {} permissions",
                updatedRole.getName(), updatedRole.getPermissions().size());

        // Auditoría
        logAuditAction(
                "ROLE_UPDATE",
                "Rol actualizado: " + updatedRole.getName() + " (Permisos: " + updatedRole.getPermissions().size()
                        + ")",
                updatedRole.getId(),
                "/api/roles/" + id,
                "PUT");

        return mapToResponse(updatedRole);
    }

    // ========================================
    // CONSULTAS
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        log.info("Getting role by id: {}", id);

        // Validar ID
        if (id == null || id <= 0) {
            throw new BadRequestException("El ID del rol debe ser un número positivo");
        }

        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));

        return mapToResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleByName(String name) {
        log.info("Getting role by name: {}", name);

        // Validar nombre
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("El nombre del rol no puede estar vacío");
        }

        if (name.trim().length() < MIN_NAME_LENGTH) {
            throw new BadRequestException("El nombre del rol debe tener al menos " + MIN_NAME_LENGTH + " caracteres");
        }

        Role role = roleRepository.findByNameWithPermissions(name.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con nombre: " + name));

        return mapToResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoleResponse> getAllRoles(Pageable pageable) {
        log.info("Getting all roles with pagination - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        validatePageable(pageable);

        Page<Role> rolePage = roleRepository.findAll(pageable);
        return mapToPageResponse(rolePage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoleResponse> getActiveRoles(Pageable pageable) {
        log.info("Getting active roles with pagination - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        validatePageable(pageable);

        Page<Role> rolePage = roleRepository.findByActiveTrue(pageable);
        return mapToPageResponse(rolePage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoleResponse> searchByFilter(String query, String type, Pageable pageable) {
        Page<Role> page;

        switch (type.toUpperCase()) {
            case "NOMBRE":
                page = roleRepository.searchByName(query, pageable);
                break;
            default: // "ALL"
                page = roleRepository.searchByName(query, pageable);
                break;
        }

        return mapToPageResponse(page);
    }

    // ========================================
    // ELIMINAR (SOFT DELETE)
    // ========================================

    @Override
    public void deleteRole(Long id) {
        log.info("Deleting role with id: {}", id);

        // Validar ID
        if (id == null || id <= 0) {
            throw new BadRequestException("El ID del rol debe ser un número positivo");
        }

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));

        // Validar que no esté ya inactivo
        if (!role.getActive()) {
            throw new ValidationException("El rol ya está desactivado");
        }

        // Validar que no tenga usuarios asignados
        if (role.getUsers() != null && !role.getUsers().isEmpty()) {
            throw new ValidationException("No se puede eliminar un rol que tiene " +
                    role.getUsers().size() + " usuario(s) asignado(s). " +
                    "Primero reasigne los usuarios a otro rol.");
        }

        // Soft delete
        role.setActive(false);
        roleRepository.save(role);

        log.info("Role soft deleted successfully: {}", role.getName());

        // Auditoría
        logAuditAction(
                "ROLE_DELETE",
                "Rol eliminado (soft delete): " + role.getName() + " (ID: " + id + ")",
                id,
                "/api/roles/" + id,
                "DELETE");
    }

    // ========================================
    // GESTIÓN DE PERMISOS
    // ========================================

    @Override
    public RoleResponse assignPermissions(Long roleId, List<Long> permissionIds) {
        log.info("Assigning {} permissions to role: {}",
                permissionIds != null ? permissionIds.size() : 0, roleId);

        // Validaciones
        if (roleId == null || roleId <= 0) {
            throw new BadRequestException("El ID del rol debe ser un número positivo");
        }

        if (permissionIds == null || permissionIds.isEmpty()) {
            throw new BadRequestException("Debe proporcionar al menos un ID de permiso");
        }

        // Validar IDs de permisos
        for (Long permissionId : permissionIds) {
            if (permissionId == null || permissionId <= 0) {
                throw new BadRequestException("Todos los IDs de permisos deben ser números positivos");
            }
        }

        // Validar que no haya duplicados
        Set<Long> uniqueIds = new HashSet<>(permissionIds);
        if (uniqueIds.size() != permissionIds.size()) {
            throw new BadRequestException("La lista de permisos contiene IDs duplicados");
        }

        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        // Validar que el rol esté activo
        if (!role.getActive()) {
            throw new ValidationException("No se pueden asignar permisos a un rol inactivo");
        }

        Set<Permission> permissions = permissionRepository.findByIdIn(new HashSet<>(permissionIds));

        if (permissions.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron permisos con los IDs proporcionados");
        }

        if (permissions.size() != permissionIds.size()) {
            throw new BadRequestException("Algunos IDs de permisos no existen en el sistema. " +
                    "Se encontraron " + permissions.size() + " de " + permissionIds.size() + " permisos solicitados");
        }

        // Validar permisos inactivos
        long inactivePermissions = permissions.stream()
                .filter(p -> !p.getActive())
                .count();
        if (inactivePermissions > 0) {
            throw new ValidationException("No se pueden asignar " + inactivePermissions +
                    " permiso(s) inactivo(s)");
        }

        // Validar límite de permisos
        int totalPermissions = role.getPermissions().size() + permissions.size();
        if (totalPermissions > MAX_PERMISSIONS_PER_ROLE) {
            throw new ValidationException("El rol no puede tener más de " + MAX_PERMISSIONS_PER_ROLE +
                    " permisos. Actualmente tiene " + role.getPermissions().size() +
                    " y está intentando agregar " + permissions.size() + " más");
        }

        // Asignar permisos (addAll evita duplicados automáticamente por ser Set)
        int sizeBefore = role.getPermissions().size();
        role.getPermissions().addAll(permissions);
        int sizeAfter = role.getPermissions().size();
        int addedCount = sizeAfter - sizeBefore;

        Role updatedRole = roleRepository.save(role);
        log.info("Permissions assigned successfully to role: {} - Added: {}, Already had: {}",
                updatedRole.getName(), addedCount, sizeBefore);

        // Auditoría
        logAuditAction(
                "ROLE_PERMISSIONS_ASSIGN",
                "Permisos asignados a rol " + updatedRole.getName() + ": " + permissionIds,
                updatedRole.getId(),
                "/api/roles/" + roleId + "/permissions",
                "POST");

        return mapToResponse(updatedRole);
    }

    @Override
    public RoleResponse removePermissions(Long roleId, List<Long> permissionIds) {
        log.info("Removing {} permissions from role: {}",
                permissionIds != null ? permissionIds.size() : 0, roleId);

        // Validaciones
        if (roleId == null || roleId <= 0) {
            throw new BadRequestException("El ID del rol debe ser un número positivo");
        }

        if (permissionIds == null || permissionIds.isEmpty()) {
            throw new BadRequestException("Debe proporcionar al menos un ID de permiso");
        }

        // Validar IDs de permisos
        for (Long permissionId : permissionIds) {
            if (permissionId == null || permissionId <= 0) {
                throw new BadRequestException("Todos los IDs de permisos deben ser números positivos");
            }
        }

        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        // Validar que el rol tenga permisos
        if (role.getPermissions().isEmpty()) {
            throw new ValidationException("El rol no tiene permisos asignados");
        }

        Set<Permission> permissionsToRemove = permissionRepository.findByIdIn(new HashSet<>(permissionIds));

        if (permissionsToRemove.isEmpty()) {
            log.warn("No se encontraron permisos válidos para remover del rol: {}", roleId);
        }

        int sizeBefore = role.getPermissions().size();
        role.getPermissions().removeAll(permissionsToRemove);
        int sizeAfter = role.getPermissions().size();
        int removedCount = sizeBefore - sizeAfter;

        Role updatedRole = roleRepository.save(role);
        log.info("Permissions removed successfully from role: {} - Removed: {}, Remaining: {}",
                updatedRole.getName(), removedCount, sizeAfter);

        // Auditoría
        logAuditAction(
                "ROLE_PERMISSIONS_REMOVE",
                "Permisos removidos de rol " + updatedRole.getName() + ": " + permissionIds,
                updatedRole.getId(),
                "/api/roles/" + roleId + "/permissions",
                "DELETE");

        return mapToResponse(updatedRole);
    }

    // ========================================
    // MÉTODOS HELPER PRIVADOS
    // ========================================

    private void logAuditAction(String action, String description, Long resourceId,
            String endpoint, String method) {
        try {
            // auditService.logAction(...) temporalmente deshabilitado por conflicto de firma
        } catch (Exception e) {
            log.error("Error logging audit for action: {}", action, e);
        }
    }

    private void validateRoleName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("El nombre del rol es obligatorio");
        }

        String trimmedName = name.trim();

        if (trimmedName.length() < MIN_NAME_LENGTH) {
            throw new BadRequestException("El nombre del rol debe tener al menos " +
                    MIN_NAME_LENGTH + " caracteres");
        }

        if (trimmedName.length() > MAX_NAME_LENGTH) {
            throw new BadRequestException("El nombre del rol no puede superar los " +
                    MAX_NAME_LENGTH + " caracteres");
        }

        // Validar que solo contenga letras, números, espacios y guiones bajos
        if (!trimmedName.matches("^[A-Za-zÁÉÍÓÚáéíóúÑñ0-9\\s_-]+$")) {
            throw new BadRequestException(
                    "El nombre del rol solo puede contener letras, números, espacios, guiones y guiones bajos");
        }
    }

    private void validateAndAssignPermissions(Role role, Set<Long> permissionIds) {
        // Validar cantidad
        if (permissionIds.size() > MAX_PERMISSIONS_PER_ROLE) {
            throw new ValidationException("No se pueden asignar más de " + MAX_PERMISSIONS_PER_ROLE +
                    " permisos a un rol. Intentó asignar: " + permissionIds.size());
        }

        // Validar IDs
        for (Long permissionId : permissionIds) {
            if (permissionId == null || permissionId <= 0) {
                throw new BadRequestException("Todos los IDs de permisos deben ser números positivos");
            }
        }

        // Buscar permisos
        Set<Permission> permissions = permissionRepository.findByIdIn(permissionIds);

        if (permissions.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron permisos con los IDs proporcionados");
        }

        if (permissions.size() != permissionIds.size()) {
            throw new BadRequestException("Algunos IDs de permisos no existen. " +
                    "Se encontraron " + permissions.size() + " de " + permissionIds.size() + " permisos");
        }

        // Validar que estén activos
        long inactiveCount = permissions.stream()
                .filter(p -> !p.getActive())
                .count();
        if (inactiveCount > 0) {
            throw new ValidationException("No se pueden asignar " + inactiveCount + " permiso(s) inactivo(s)");
        }

        role.getPermissions().addAll(permissions);
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

    // ========================================
    // MAPEO
    // ========================================

    private PageResponse<RoleResponse> mapToPageResponse(Page<Role> page) {
        List<RoleResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<RoleResponse>builder()
                .content(content)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .pageSize(page.getSize())
                .pageNumber(page.getNumber())
                .isLast(page.isLast())
                .build();
    }

    private RoleResponse mapToResponse(Role role) {
        Set<PermissionResponse> permissionResponses = role.getPermissions().stream()
                .map(permission -> PermissionResponse.builder()
                        .id(permission.getId())
                        .name(permission.getName())
                        .description(permission.getDescription())
                        .module(permission.getModule())
                        .active(permission.getActive())
                        .createdAt(permission.getCreatedAt())
                        .build())
                .collect(Collectors.toSet());

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .active(role.getActive())
                .permissions(permissionResponses)
                .userCount(role.getUsers() != null ? role.getUsers().size() : 0)
                .idAcceso(role.getAcceso() != null ? role.getAcceso().getId() : null)
                .nombreAcceso(role.getAcceso() != null ? role.getAcceso().getNombre() : null)
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
}
