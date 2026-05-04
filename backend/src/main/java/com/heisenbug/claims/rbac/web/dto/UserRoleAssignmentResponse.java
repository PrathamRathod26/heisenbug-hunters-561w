package com.heisenbug.claims.rbac.web.dto;

import com.heisenbug.claims.rbac.domain.AssignmentStatus;
import com.heisenbug.claims.rbac.domain.RoleName;

import java.time.Instant;
import java.util.UUID;

public record UserRoleAssignmentResponse(
        UUID id,
        UUID roleId,
        RoleName roleName,
        AssignmentStatus status,
        String note,
        String grantedByEmail,
        Instant grantedAt,
        Instant revokedAt,
        String revokedByEmail
) {
}
