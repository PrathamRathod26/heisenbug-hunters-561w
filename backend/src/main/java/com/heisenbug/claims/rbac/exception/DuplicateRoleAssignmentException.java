package com.heisenbug.claims.rbac.exception;

import com.heisenbug.claims.common.exception.ResourceConflictException;

import java.util.UUID;

public class DuplicateRoleAssignmentException extends ResourceConflictException {
    public DuplicateRoleAssignmentException(UUID userId, UUID roleId) {
        super("DUPLICATE_ROLE_ASSIGNMENT",
                "User '%s' already has an active assignment for role '%s'".formatted(userId, roleId));
    }
}
