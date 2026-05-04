package com.heisenbug.claims.rbac.exception;

import com.heisenbug.claims.common.exception.ResourceConflictException;

public class DuplicateUserException extends ResourceConflictException {
    public DuplicateUserException(String field, String value) {
        super("DUPLICATE_USER",
                "User with %s '%s' already exists".formatted(field, value));
    }
}
