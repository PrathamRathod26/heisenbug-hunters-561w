package com.heisenbug.claims.rbac.exception;

import com.heisenbug.claims.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class RoleAssignmentNotFoundException extends ResourceNotFoundException {
    public RoleAssignmentNotFoundException(UUID id) {
        super("UserRoleAssignment", id.toString());
    }
}
