package com.heisenbug.claims.rbac.exception;

import com.heisenbug.claims.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(UUID id) {
        super("User", id.toString());
    }

    public UserNotFoundException(String identifier) {
        super("User", identifier);
    }
}
