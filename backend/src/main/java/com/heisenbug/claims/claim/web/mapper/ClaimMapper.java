package com.heisenbug.claims.claim.web.mapper;

import com.heisenbug.claims.claim.domain.Claim;
import com.heisenbug.claims.claim.domain.ClaimEvidence;
import com.heisenbug.claims.claim.web.dto.ClaimCreateRequest;
import com.heisenbug.claims.claim.web.dto.ClaimEvidenceCreateDto;
import com.heisenbug.claims.claim.web.dto.ClaimEvidenceResponse;
import com.heisenbug.claims.claim.web.dto.ClaimResponse;
import com.heisenbug.claims.claim.web.dto.ClaimSummaryResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface ClaimMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "evidences", source = "evidences")
    Claim toEntity(ClaimCreateRequest req);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "claim", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    @Mapping(target = "contentType", ignore = true)
    @Mapping(target = "sizeBytes", ignore = true)
    ClaimEvidence toEntity(ClaimEvidenceCreateDto dto);

    ClaimResponse toResponse(Claim claim);

    List<ClaimEvidenceResponse> toEvidenceResponses(List<ClaimEvidence> evidences);

    ClaimEvidenceResponse toEvidenceResponse(ClaimEvidence evidence);

    ClaimSummaryResponse toSummary(Claim claim);

    @AfterMapping
    default void linkEvidencesToClaim(@MappingTarget Claim claim) {
        Optional.ofNullable(claim.getEvidences())
                .orElseGet(List::of)
                .forEach(ev -> ev.setClaim(claim));
    }
}
