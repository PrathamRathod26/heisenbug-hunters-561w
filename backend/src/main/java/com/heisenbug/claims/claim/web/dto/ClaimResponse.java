package com.heisenbug.claims.claim.web.dto;

import com.heisenbug.claims.claim.domain.ClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClaimResponse(
        UUID id,
        String policyNumber,
        String claimantName,
        ClaimStatus status,
        BigDecimal estimatedLoss,
        List<ClaimEvidenceResponse> evidences,
        Instant createdAt,
        Instant updatedAt
) {
}
