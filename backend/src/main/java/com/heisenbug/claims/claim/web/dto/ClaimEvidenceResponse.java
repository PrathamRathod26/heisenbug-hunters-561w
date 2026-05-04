package com.heisenbug.claims.claim.web.dto;

import com.heisenbug.claims.claim.domain.EvidenceType;

import java.time.Instant;
import java.util.UUID;

public record ClaimEvidenceResponse(
        UUID id,
        EvidenceType type,
        String uri,
        Instant uploadedAt
) {
}
