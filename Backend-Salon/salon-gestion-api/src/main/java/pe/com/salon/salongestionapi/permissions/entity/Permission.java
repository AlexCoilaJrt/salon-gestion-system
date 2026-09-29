package pe.com.salon.salongestionapi.permissions.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

import pe.com.salon.salongestionapi.menu.entity.AccesoMain;

@Entity
@Table(name = "reha_permissions")
public class Permission {

    public Permission() {
    }

    public Permission(Long id, String name, String description, String module, AccesoMain acceso, Boolean active,
            LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.module = module;
        this.acceso = acceso;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static class PermissionBuilder {
        private Long id;
        private String name;
        private String description;
        private String module;
        private AccesoMain acceso;
        private Boolean active = true;
        private LocalDateTime createdAt;

        public PermissionBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PermissionBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PermissionBuilder description(String description) {
            this.description = description;
            return this;
        }

        public PermissionBuilder module(String module) {
            this.module = module;
            return this;
        }

        public PermissionBuilder acceso(AccesoMain acceso) {
            this.acceso = acceso;
            return this;
        }

        public PermissionBuilder active(Boolean active) {
            this.active = active;
            return this;
        }

        public PermissionBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Permission build() {
            return new Permission(id, name, description, module, acceso, active, createdAt);
        }
    }

    public static PermissionBuilder builder() {
        return new PermissionBuilder();
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

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public AccesoMain getAcceso() {
        return acceso;
    }

    public void setAcceso(AccesoMain acceso) {
        this.acceso = acceso;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Permission that = (Permission) o;
        return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    @EqualsAndHashCode.Include
    private String name;

    @Column(length = 200)
    private String description;

    @Column(length = 50)
    private String module;

    @ManyToOne
    @JoinColumn(name = "id_acceso", nullable = false)
    private AccesoMain acceso;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
