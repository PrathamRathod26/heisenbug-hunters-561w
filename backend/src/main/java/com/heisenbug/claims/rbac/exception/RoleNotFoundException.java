package com.heisenbug.claims.rbac.exception;

import com.heisenbug.claims.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class RoleNotFoundException extends ResourceNotFoundException {
    public RoleNotFoundException(UUID id) {
        super("Role", id.toString());
    }

    public RoleNotFoundException(String identifier) {
        super("Role", identifier);
    }
}
