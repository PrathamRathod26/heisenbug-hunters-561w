package com.heisenbug.claims.rbac.exception;

import com.heisenbug.claims.common.exception.ResourceConflictException;
import com.heisenbug.claims.rbac.domain.LineOfBusiness;

import java.util.UUID;

public class DuplicateDoaMatrixException extends ResourceConflictException {
    public DuplicateDoaMatrixException(UUID roleId, LineOfBusiness lob, String geo) {
        super("DUPLICATE_DOA_MATRIX",
                "DOA matrix for role '%s', LOB '%s', geo '%s' already exists"
                        .formatted(roleId, lob, geo));
    }
}
