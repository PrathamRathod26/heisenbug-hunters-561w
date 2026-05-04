package com.heisenbug.claims.rbac.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "app_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_user_external_id", columnNames = "external_id")
        },
        indexes = {
                @Index(name = "idx_user_type", columnList = "user_type"),
                @Index(name = "idx_user_status", columnList = "status")
        })
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 16)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "licence_id", length = 64)
    private String licenceId;

    @Column(name = "licence_expires_at")
    private Instant licenceExpiresAt;

    @OneToMany(mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<UserRoleAssignment> roleAssignments = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected User() {
    }

    public User(String externalId,
                String email,
                String displayName,
                UserType userType) {
        this.externalId = externalId;
        this.email = email;
        this.displayName = displayName;
        this.userType = userType;
    }

    public void addRoleAssignment(UserRoleAssignment assignment) {
        roleAssignments.add(assignment);
        assignment.setUser(this);
    }

    public void removeRoleAssignment(UserRoleAssignment assignment) {
        roleAssignments.remove(assignment);
        assignment.setUser(null);
    }

    public UUID getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public UserType getUserType() { return userType; }
    public UserStatus getStatus() { return status; }
    public String getLicenceId() { return licenceId; }
    public Instant getLicenceExpiresAt() { return licenceExpiresAt; }
    public List<UserRoleAssignment> getRoleAssignments() { return roleAssignments; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void setEmail(String email) { this.email = email; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setStatus(UserStatus status) { this.status = status; }
    public void setLicenceId(String licenceId) { this.licenceId = licenceId; }
    public void setLicenceExpiresAt(Instant licenceExpiresAt) { this.licenceExpiresAt = licenceExpiresAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
