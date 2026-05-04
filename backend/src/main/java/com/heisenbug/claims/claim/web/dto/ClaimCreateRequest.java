package com.heisenbug.claims.claim.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ClaimCreateRequest(

        @NotBlank(message = "policyNumber is required")
        @Pattern(regexp = "^[A-Z0-9-]{6,32}$",
                message = "policyNumber must be 6-32 chars of A-Z, 0-9 or '-'")
        String policyNumber,

        @NotBlank(message = "claimantName is required")
        @Size(min = 2, max = 120, message = "claimantName must be 2-120 characters")
        String claimantName,

        @NotNull(message = "estimatedLoss is required")
        @DecimalMin(value = "0.00", inclusive = true,
                message = "estimatedLoss must be non-negative")
        @Digits(integer = 12, fraction = 2,
                message = "estimatedLoss must have at most 12 digits and 2 decimals")
        BigDecimal estimatedLoss,

        @Valid
        List<ClaimEvidenceCreateDto> evidences
) {
}
