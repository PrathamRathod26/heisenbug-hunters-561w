package com.heisenbug.claims.claim.repository;

import com.heisenbug.claims.claim.domain.Claim;
import com.heisenbug.claims.claim.domain.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    @EntityGraph(attributePaths = "evidences")
    Optional<Claim> findWithEvidencesById(UUID id);

    @EntityGraph(attributePaths = "evidences")
    @Query("select distinct c from Claim c where c.status = :status")
    List<Claim> findAllWithEvidencesByStatus(@Param("status") ClaimStatus status);

    Page<Claim> findAllByStatus(ClaimStatus status, Pageable pageable);

    long countByStatus(ClaimStatus status);
}
