package com.heisenbug.claims.claim.repository;

import com.heisenbug.claims.claim.domain.ClaimEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClaimEvidenceRepository extends JpaRepository<ClaimEvidence, UUID> {
}
