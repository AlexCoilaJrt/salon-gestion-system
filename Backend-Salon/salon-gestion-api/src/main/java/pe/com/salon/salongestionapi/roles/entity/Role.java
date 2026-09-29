package pe.com.salon.salongestionapi.roles.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import pe.com.salon.salongestionapi.permissions.entity.Permission;
import pe.com.salon.salongestionapi.security.entity.Usuario;
import pe.com.salon.salongestionapi.menu.entity.AccesoMain;

@Entity
@Table(name = "reha_roles")
public class Role {

    public Role() {
    }

    public Role(Long id, String name, String description, Boolean active, Set<Permission> permissions,
            Set<Usuario> users, AccesoMain acceso, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.active = active;
        this.permissions = permissions;
        this.users = users;
        this.acceso = acceso;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static class RoleBuilder {
        private Long id;
        private String name;
        private String description;
        private Boolean active = true;
        private Set<Permission> permissions = new java.util.HashSet<>();
        private Set<Usuario> users = new java.util.HashSet<>();
        private AccesoMain acceso;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public RoleBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public RoleBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RoleBuilder description(String description) {
            this.description = description;
            return this;
        }

        public RoleBuilder active(Boolean active) {
            this.active = active;
            return this;
        }

        public RoleBuilder permissions(Set<Permission> permissions) {
            this.permissions = permissions;
            return this;
        }

        public RoleBuilder users(Set<Usuario> users) {
            this.users = users;
            return this;
        }

        public RoleBuilder acceso(AccesoMain acceso) {
            this.acceso = acceso;
            return this;
        }

        public RoleBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public RoleBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Role build() {
            return new Role(id, name, description, active, permissions, users, acceso, createdAt, updatedAt);
        }
    }

    public static RoleBuilder builder() {
        return new RoleBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Usuario> getUsers() {
        return users;
    }

    public void setUsers(Set<Usuario> users) {
        this.users = users;
    }

    public AccesoMain getAcceso() {
        return acceso;
    }

    public void setAcceso(AccesoMain acceso) {
        this.acceso = acceso;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Role role = (Role) o;
        return java.util.Objects.equals(id, role.id) && java.util.Objects.equals(name, role.name);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    @EqualsAndHashCode.Include
    private String name;

    @Column(length = 200)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @Builder.Default
    @JoinTable(name = "reha_role_permissions", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();
    @ManyToMany(mappedBy = "roles")
    @Builder.Default
    private Set<Usuario> users = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "id_acceso", nullable = false)
    private AccesoMain acceso;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
