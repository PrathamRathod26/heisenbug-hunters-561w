package com.heisenbug.claims.rbac.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_role_assignment", indexes = {
        @Index(name = "idx_ura_user", columnList = "user_id"),
        @Index(name = "idx_ura_role", columnList = "role_id"),
        @Index(name = "idx_ura_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class UserRoleAssignment {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AssignmentStatus status = AssignmentStatus.ACTIVE;

    @Column(length = 255)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by_user_id")
    private User grantedBy;

    @CreatedDate
    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by_user_id")
    private User revokedBy;

    protected UserRoleAssignment() {
    }

    public UserRoleAssignment(User user, Role role, User grantedBy, String note) {
        this.user = user;
        this.role = role;
        this.grantedBy = grantedBy;
        this.note = note;
    }

    public void revoke(User revokedByUser) {
        if (status == AssignmentStatus.REVOKED) {
            return;
        }
        this.status = AssignmentStatus.REVOKED;
        this.revokedBy = revokedByUser;
        this.revokedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Role getRole() { return role; }
    public AssignmentStatus getStatus() { return status; }
    public String getNote() { return note; }
    public User getGrantedBy() { return grantedBy; }
    public Instant getGrantedAt() { return grantedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public User getRevokedBy() { return revokedBy; }

    public void setUser(User user) { this.user = user; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRoleAssignment other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
