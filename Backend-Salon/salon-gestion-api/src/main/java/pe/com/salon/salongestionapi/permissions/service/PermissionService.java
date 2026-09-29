package pe.com.salon.salongestionapi.permissions.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import pe.com.salon.salongestionapi.permissions.dto.PermissionRequest;
import pe.com.salon.salongestionapi.permissions.dto.PermissionResponse;
import pe.com.salon.salongestionapi.shared.PageResponse;

public interface PermissionService {

    // ========== CRUD BÁSICO ==========

    PermissionResponse createPermission(PermissionRequest request);

    PermissionResponse updatePermission(Long id, PermissionRequest request);

    void deletePermission(Long id);

    // ========== CONSULTAS INDIVIDUALES ==========

    PermissionResponse getPermissionById(Long id);

    PermissionResponse getPermissionByName(String name);

    // ========== LISTADOS CON PAGINACIÓN ==========

    PageResponse<PermissionResponse> getAllPermissions(Pageable pageable);

    PageResponse<PermissionResponse> getActivePermissions(Pageable pageable);

    PageResponse<PermissionResponse> getPermissionsByModule(String module, Pageable pageable);

    PageResponse<PermissionResponse> getActivePermissionsByModule(String module, Pageable pageable);

    PageResponse<PermissionResponse> searchPermissions(String search, String target, Pageable pageable);

    PageResponse<PermissionResponse> searchByFilter(String query, String type, Pageable pageable);

    // ========== LISTADOS SIN PAGINACIÓN (para selects/dropdowns) ==========

    List<PermissionResponse> getAllPermissionsAsList();

    List<PermissionResponse> getActivePermissionsAsList();

    List<PermissionResponse> getPermissionsByModuleAsList(String module);

    List<PermissionResponse> getActivePermissionsByModuleAsList(String module);

    List<PermissionResponse> searchPermissionsAsList(String search);

    // ========== MÓDULOS ==========

    List<String> getAllModules();

    // ========== UTILIDADES ==========

    boolean existsByName(String name);

}
