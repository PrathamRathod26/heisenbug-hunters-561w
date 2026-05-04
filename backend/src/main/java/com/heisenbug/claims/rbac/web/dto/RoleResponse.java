package com.heisenbug.claims.rbac.web.dto;

import com.heisenbug.claims.rbac.domain.RoleName;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        RoleName name,
        String description,
        boolean active,
        Set<PermissionResponse> permissions,
        Instant createdAt,
        Instant updatedAt
) {
}
