package com.heisenbug.claims.rbac.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UserRoleAssignRequest(

        @NotNull(message = "roleId is required")
        UUID roleId,

        @Size(max = 255, message = "note must be at most 255 characters")
        String note,

        UUID grantedByUserId
) {
}
