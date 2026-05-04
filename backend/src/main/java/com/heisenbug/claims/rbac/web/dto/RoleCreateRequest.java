package com.heisenbug.claims.rbac.web.dto;

import com.heisenbug.claims.rbac.domain.RoleName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record RoleCreateRequest(

        @NotNull(message = "name is required")
        RoleName name,

        @Size(max = 255, message = "description must be at most 255 characters")
        String description,

        Set<UUID> permissionIds
) {
}
