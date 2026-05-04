package com.heisenbug.claims.rbac.web.dto;

import com.heisenbug.claims.rbac.domain.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UserCreateRequest(

        @NotBlank(message = "externalId is required")
        @Size(max = 128, message = "externalId must be at most 128 characters")
        String externalId,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        @Size(max = 254, message = "email must be at most 254 characters")
        String email,

        @NotBlank(message = "displayName is required")
        @Size(min = 2, max = 120, message = "displayName must be 2-120 characters")
        String displayName,

        @NotNull(message = "userType is required")
        UserType userType,

        @Size(max = 64, message = "licenceId must be at most 64 characters")
        String licenceId,

        Instant licenceExpiresAt
) {
}
