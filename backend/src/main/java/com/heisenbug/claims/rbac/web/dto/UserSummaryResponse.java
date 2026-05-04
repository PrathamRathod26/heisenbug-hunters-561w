package com.heisenbug.claims.rbac.web.dto;

import com.heisenbug.claims.rbac.domain.UserStatus;
import com.heisenbug.claims.rbac.domain.UserType;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String email,
        String displayName,
        UserType userType,
        UserStatus status
) {
}
