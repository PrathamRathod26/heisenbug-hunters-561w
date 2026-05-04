package com.heisenbug.claims.rbac.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record RolePermissionUpdateRequest(

        @NotNull(message = "permissionIds is required")
        @NotEmpty(message = "permissionIds must not be empty")
        Set<UUID> permissionIds
) {
}
