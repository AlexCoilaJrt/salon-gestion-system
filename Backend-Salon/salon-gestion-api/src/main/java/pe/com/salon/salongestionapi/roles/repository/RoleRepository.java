package pe.com.salon.salongestionapi.roles.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pe.com.salon.salongestionapi.roles.entity.Role;
import pe.com.salon.salongestionapi.permissions.entity.Permission;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    Page<Role> findByActiveTrue(Pageable pageable);

    boolean existsByName(String name);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<Role> findByIdWithPermissions(@Param("id") Long id);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.name = :name")
    Optional<Role> findByNameWithPermissions(@Param("name") String name);

    @Query("SELECT p FROM Permission p WHERE p.module = :module")
    Page<Permission> findByModule(@Param("module") String module, Pageable pageable);

    @Query("SELECT p FROM Permission p WHERE p.module = :module ORDER BY p.name ASC")
    List<Permission> findByModuleOrderByName(@Param("module") String module);

    @Query("SELECT r FROM Role r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Role> searchByName(@Param("name") String name, Pageable pageable);
}
