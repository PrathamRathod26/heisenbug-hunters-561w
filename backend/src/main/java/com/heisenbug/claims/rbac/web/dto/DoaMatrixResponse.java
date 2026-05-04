package com.heisenbug.claims.rbac.web.dto;

import com.heisenbug.claims.rbac.domain.LineOfBusiness;
import com.heisenbug.claims.rbac.domain.RoleName;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public record DoaMatrixResponse(
        UUID id,
        UUID roleId,
        RoleName roleName,
        LineOfBusiness lineOfBusiness,
        String geo,
        BigInteger approveUpToPaise,
        BigInteger fourEyeAbovePaise,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
