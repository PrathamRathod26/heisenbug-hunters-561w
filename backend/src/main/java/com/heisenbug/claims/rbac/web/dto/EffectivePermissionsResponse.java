package com.heisenbug.claims.rbac.web.dto;

import com.heisenbug.claims.rbac.domain.CapabilityCode;
import com.heisenbug.claims.rbac.domain.PermissionScope;
import com.heisenbug.claims.rbac.domain.RoleName;

import java.util.Set;
import java.util.UUID;

public record EffectivePermissionsResponse(
        UUID userId,
        Set<RoleName> activeRoles,
        Set<CapabilityPermission> permissions
) {
    public record CapabilityPermission(
            CapabilityCode capability,
            PermissionScope effectiveScope,
            Set<RoleName> grantedBy
    ) {
    }
}
