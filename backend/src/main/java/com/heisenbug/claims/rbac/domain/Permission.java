package com.heisenbug.claims.rbac.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "permission",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_permission_capability_scope",
                columnNames = {"capability", "scope"}))
public class Permission {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private CapabilityCode capability;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PermissionScope scope;

    @Column(length = 255)
    private String description;

    protected Permission() {
    }

    public Permission(CapabilityCode capability, PermissionScope scope, String description) {
        this.capability = capability;
        this.scope = scope;
        this.description = description;
    }

    public UUID getId() { return id; }
    public CapabilityCode getCapability() { return capability; }
    public PermissionScope getScope() { return scope; }
    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
