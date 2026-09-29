package pe.com.salon.salongestionapi.menu.repository;

import pe.com.salon.salongestionapi.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    
    /**
     * Obtener items raíz (sin parent) filtrados por acceso y ordenados
     */
    @Query("SELECT m FROM MenuItem m WHERE m.parent IS NULL AND m.acceso.id = :accesoId AND m.activo = true ORDER BY m.orden")
    List<MenuItem> findRootItemsByAcceso(@Param("accesoId") Long accesoId);

    /**
     * Obtener items raíz (sin parent) ordenados
     */
    @Query("SELECT m FROM MenuItem m WHERE m.parent IS NULL AND m.activo = true ORDER BY m.orden")
    List<MenuItem> findRootItems();
    
    /**
     * Obtener hijos de un item específico
     */
    @Query("SELECT m FROM MenuItem m WHERE m.parent.id = :parentId AND m.activo = true ORDER BY m.orden")
    List<MenuItem> findByParentId(@Param("parentId") Long parentId);
    
    /**
     * Buscar por título y padre (para evitar duplicados)
     */
    java.util.List<MenuItem> findByTituloAndParent(String titulo, MenuItem parent);

    /**
     * Buscar por título (para grupos raíz)
     */
    java.util.List<MenuItem> findByTituloAndParentIsNull(String titulo);

    /**
     * Buscar por título y acceso (para grupos raíz específicos de un portal)
     */
    java.util.List<MenuItem> findByTituloAndParentIsNullAndAccesoId(String titulo, Long accesoId);

    /**
     * Obtener todos los items activos
     */
    List<MenuItem> findByActivoTrueOrderByOrdenAsc();
}
