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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "doa_matrix",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_doa_role_lob_geo",
                columnNames = {"role_id", "line_of_business", "geo"}),
        indexes = {
                @Index(name = "idx_doa_role", columnList = "role_id"),
                @Index(name = "idx_doa_active", columnList = "active")
        })
@EntityListeners(AuditingEntityListener.class)
public class DoaMatrix {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_of_business", nullable = false, length = 24)
    private LineOfBusiness lineOfBusiness;

    @Column(nullable = false, length = 16)
    private String geo;

    @Column(name = "approve_up_to_paise", nullable = false, precision = 20, scale = 0)
    private BigInteger approveUpToPaise;

    @Column(name = "four_eye_above_paise", nullable = false, precision = 20, scale = 0)
    private BigInteger fourEyeAbovePaise;

    @Column(nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected DoaMatrix() {
    }

    public DoaMatrix(Role role,
                     LineOfBusiness lineOfBusiness,
                     String geo,
                     BigInteger approveUpToPaise,
                     BigInteger fourEyeAbovePaise) {
        this.role = role;
        this.lineOfBusiness = lineOfBusiness;
        this.geo = geo;
        this.approveUpToPaise = approveUpToPaise;
        this.fourEyeAbovePaise = fourEyeAbovePaise;
    }

    public UUID getId() { return id; }
    public Role getRole() { return role; }
    public LineOfBusiness getLineOfBusiness() { return lineOfBusiness; }
    public String getGeo() { return geo; }
    public BigInteger getApproveUpToPaise() { return approveUpToPaise; }
    public BigInteger getFourEyeAbovePaise() { return fourEyeAbovePaise; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void setApproveUpToPaise(BigInteger approveUpToPaise) { this.approveUpToPaise = approveUpToPaise; }
    public void setFourEyeAbovePaise(BigInteger fourEyeAbovePaise) { this.fourEyeAbovePaise = fourEyeAbovePaise; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoaMatrix other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
