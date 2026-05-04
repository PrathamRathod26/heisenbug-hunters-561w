package com.heisenbug.claims.claim.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "claim_analysis",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_claim_analysis_claim",
                columnNames = "claim_id"),
        indexes = {
                @Index(name = "idx_claim_analysis_claim", columnList = "claim_id"),
                @Index(name = "idx_claim_analysis_severity", columnList = "severity_verdict")
        })
@EntityListeners(AuditingEntityListener.class)
public class ClaimAnalysis {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(name = "model_version", length = 128)
    private String modelVersion;

    @Column(name = "total_detections")
    private Integer totalDetections;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @Column(name = "severity_verdict", length = 32)
    private String severityVerdict;

    @Column(name = "repair_recommendation", length = 32)
    private String repairRecommendation;

    @Column(name = "cost_total_paise")
    private Long costTotalPaise;

    @Column(name = "assessment_confidence")
    private Double assessmentConfidence;

    @Column(name = "payload_json", columnDefinition = "TEXT", nullable = false)
    private String payloadJson;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ClaimAnalysis() {
    }

    public ClaimAnalysis(Claim claim, String payloadJson) {
        this.claim = claim;
        this.payloadJson = payloadJson;
    }

    public UUID getId() { return id; }
    public Claim getClaim() { return claim; }
    public String getModelVersion() { return modelVersion; }
    public Integer getTotalDetections() { return totalDetections; }
    public Integer getProcessingTimeMs() { return processingTimeMs; }
    public String getSeverityVerdict() { return severityVerdict; }
    public String getRepairRecommendation() { return repairRecommendation; }
    public Long getCostTotalPaise() { return costTotalPaise; }
    public Double getAssessmentConfidence() { return assessmentConfidence; }
    public String getPayloadJson() { return payloadJson; }
    public Instant getCreatedAt() { return createdAt; }

    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public void setTotalDetections(Integer totalDetections) { this.totalDetections = totalDetections; }
    public void setProcessingTimeMs(Integer processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    public void setSeverityVerdict(String severityVerdict) { this.severityVerdict = severityVerdict; }
    public void setRepairRecommendation(String repairRecommendation) { this.repairRecommendation = repairRecommendation; }
    public void setCostTotalPaise(Long costTotalPaise) { this.costTotalPaise = costTotalPaise; }
    public void setAssessmentConfidence(Double assessmentConfidence) { this.assessmentConfidence = assessmentConfidence; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClaimAnalysis other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
