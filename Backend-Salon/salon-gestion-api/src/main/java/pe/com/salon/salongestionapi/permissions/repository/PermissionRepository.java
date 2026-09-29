package pe.com.salon.salongestionapi.permissions.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pe.com.salon.salongestionapi.permissions.entity.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

        // ========== BÚSQUEDA INDIVIDUAL ==========

        Optional<Permission> findByName(String name);

        boolean existsByName(String name);

        // ========== BÚSQUEDA POR CONJUNTO DE IDS ==========

        Set<Permission> findByIdIn(Set<Long> ids);

        // ========== LISTADOS SIN PAGINACIÓN ==========

        List<Permission> findByActiveTrue();

        List<Permission> findByActive(Boolean active);

        List<Permission> findByModuleAndActive(String module, Boolean active);

        @Query("SELECT p FROM Permission p WHERE p.module = :module ORDER BY p.name ASC")
        List<Permission> findByModuleOrderByName(@Param("module") String module);

        @Query("SELECT p FROM Permission p WHERE " +
                        "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))")
        List<Permission> searchPermissions(@Param("search") String search);

        // ========== LISTADOS CON PAGINACIÓN ==========

        Page<Permission> findByActive(Boolean active, Pageable pageable);

        Page<Permission> findByModule(String module, Pageable pageable);

        Page<Permission> findByModuleAndActive(String module, Boolean active, Pageable pageable);

        @Query("SELECT p FROM Permission p WHERE " +
                        "(:target = 'name' AND LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:target = 'desc' AND LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:target = 'all' AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))))")
        Page<Permission> searchPermissionsAdvanced(
                        @Param("search") String search,
                        @Param("target") String target,
                        Pageable pageable);
        // ========== CONSULTAS ESPECIALES ==========

        @Query("SELECT DISTINCT p.module FROM Permission p WHERE p.active = true ORDER BY p.module ASC")
        List<String> findDistinctModules();

        @Query("SELECT COUNT(p) FROM Permission p WHERE p.module = :module AND p.active = true")
        Long countActiveByModule(@Param("module") String module);

        @Query("SELECT COUNT(p) FROM Permission p WHERE p.active = true")
        Long countActive();
}
