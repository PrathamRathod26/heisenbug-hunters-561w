package com.heisenbug.claims.rbac.exception;

import com.heisenbug.claims.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class DoaMatrixNotFoundException extends ResourceNotFoundException {
    public DoaMatrixNotFoundException(UUID id) {
        super("DoaMatrix", id.toString());
    }

    public DoaMatrixNotFoundException(String descriptor) {
        super("DoaMatrix", descriptor);
    }
}
