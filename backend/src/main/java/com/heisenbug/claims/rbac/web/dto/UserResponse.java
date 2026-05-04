package com.heisenbug.claims.rbac.web.dto;

import com.heisenbug.claims.rbac.domain.UserStatus;
import com.heisenbug.claims.rbac.domain.UserType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String externalId,
        String email,
        String displayName,
        UserType userType,
        UserStatus status,
        String licenceId,
        Instant licenceExpiresAt,
        List<UserRoleAssignmentResponse> roleAssignments,
        Instant createdAt,
        Instant updatedAt
) {
}
