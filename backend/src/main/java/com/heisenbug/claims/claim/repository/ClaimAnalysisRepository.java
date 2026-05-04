package com.heisenbug.claims.claim.repository;

import com.heisenbug.claims.claim.domain.ClaimAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClaimAnalysisRepository extends JpaRepository<ClaimAnalysis, UUID> {

    Optional<ClaimAnalysis> findByClaimId(UUID claimId);

    boolean existsByClaimId(UUID claimId);

    void deleteByClaimId(UUID claimId);
}
