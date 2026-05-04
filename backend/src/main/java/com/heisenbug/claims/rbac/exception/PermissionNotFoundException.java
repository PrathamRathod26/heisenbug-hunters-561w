package com.heisenbug.claims.rbac.exception;

import com.heisenbug.claims.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class PermissionNotFoundException extends ResourceNotFoundException {
    public PermissionNotFoundException(UUID id) {
        super("Permission", id.toString());
    }
}
