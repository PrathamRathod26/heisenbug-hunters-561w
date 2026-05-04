package com.heisenbug.claims.rbac.exception;

import com.heisenbug.claims.common.exception.ResourceConflictException;
import com.heisenbug.claims.rbac.domain.RoleName;

public class DuplicateRoleException extends ResourceConflictException {
    public DuplicateRoleException(RoleName name) {
        super("DUPLICATE_ROLE",
                "Role '%s' already exists".formatted(name));
    }
}
