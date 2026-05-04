package com.heisenbug.claims.rbac.web.dto;

import com.heisenbug.claims.rbac.domain.UserStatus;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UserUpdateRequest(

        @Size(min = 2, max = 120, message = "displayName must be 2-120 characters")
        String displayName,

        UserStatus status,

        @Size(max = 64, message = "licenceId must be at most 64 characters")
        String licenceId,

        Instant licenceExpiresAt
) {
}
