package com.heisenbug.claims.claim.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "claim", indexes = {
        @Index(name = "idx_claim_status", columnList = "status"),
        @Index(name = "idx_claim_policy", columnList = "policy_number")
})
@EntityListeners(AuditingEntityListener.class)
public class Claim {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "policy_number", nullable = false, length = 32)
    private String policyNumber;

    @Column(name = "claimant_name", nullable = false, length = 120)
    private String claimantName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ClaimStatus status;

    @Column(name = "estimated_loss", nullable = false, precision = 14, scale = 2)
    private BigDecimal estimatedLoss;

    @OneToMany(mappedBy = "claim",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ClaimEvidence> evidences = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Claim() {
    }

    public Claim(String policyNumber, String claimantName, ClaimStatus status, BigDecimal estimatedLoss) {
        this.policyNumber = policyNumber;
        this.claimantName = claimantName;
        this.status = status;
        this.estimatedLoss = estimatedLoss;
    }

    public void addEvidence(ClaimEvidence evidence) {
        evidences.add(evidence);
        evidence.setClaim(this);
    }

    public void removeEvidence(ClaimEvidence evidence) {
        evidences.remove(evidence);
        evidence.setClaim(null);
    }

    public UUID getId() { return id; }
    public String getPolicyNumber() { return policyNumber; }
    public String getClaimantName() { return claimantName; }
    public ClaimStatus getStatus() { return status; }
    public BigDecimal getEstimatedLoss() { return estimatedLoss; }
    public List<ClaimEvidence> getEvidences() { return evidences; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    public void setClaimantName(String claimantName) { this.claimantName = claimantName; }
    public void setStatus(ClaimStatus status) { this.status = status; }
    public void setEstimatedLoss(BigDecimal estimatedLoss) { this.estimatedLoss = estimatedLoss; }
    public void setEvidences(List<ClaimEvidence> evidences) { this.evidences = evidences; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Claim other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
