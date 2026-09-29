package pe.com.salon.salongestionapi.roles.service;

import java.util.List;
import org.springframework.data.domain.Pageable;
import pe.com.salon.salongestionapi.roles.dto.RoleRequest;
import pe.com.salon.salongestionapi.roles.dto.RoleResponse;
import pe.com.salon.salongestionapi.shared.PageResponse;

public interface RoleService {

    RoleResponse createRole(RoleRequest request);

    RoleResponse updateRole(Long id, RoleRequest request);

    RoleResponse getRoleById(Long id);

    RoleResponse getRoleByName(String name);

    // --- CAMBIOS PARA PAGINACIÓN ---

    PageResponse<RoleResponse> getAllRoles(Pageable pageable);

    PageResponse<RoleResponse> getActiveRoles(Pageable pageable);

    PageResponse<RoleResponse> searchByFilter(String query, String type, Pageable pageable);

    void deleteRole(Long id);

    RoleResponse assignPermissions(Long roleId, List<Long> permissionIds);

    RoleResponse removePermissions(Long roleId, List<Long> permissionIds);
}
