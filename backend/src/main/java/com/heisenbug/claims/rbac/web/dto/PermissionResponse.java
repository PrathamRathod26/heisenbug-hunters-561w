package com.heisenbug.claims.rbac.web.dto;

import com.heisenbug.claims.rbac.domain.CapabilityCode;
import com.heisenbug.claims.rbac.domain.PermissionScope;

import java.util.UUID;

public record PermissionResponse(
        UUID id,
        CapabilityCode capability,
        PermissionScope scope,
        String description
) {
}
