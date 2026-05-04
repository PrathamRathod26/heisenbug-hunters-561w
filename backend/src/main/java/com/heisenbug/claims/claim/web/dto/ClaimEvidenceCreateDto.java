package com.heisenbug.claims.claim.web.dto;

import com.heisenbug.claims.claim.domain.EvidenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record ClaimEvidenceCreateDto(

        @NotNull(message = "evidence type is required")
        EvidenceType type,

        @NotBlank(message = "evidence uri is required")
        @Size(max = 512, message = "uri must be at most 512 characters")
        @URL(message = "uri must be a valid URL")
        String uri
) {
}
