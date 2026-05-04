package com.heisenbug.claims.common.exception;

public abstract class ResourceConflictException extends RuntimeException {

    private final String conflictCode;

    protected ResourceConflictException(String conflictCode, String message) {
        super(message);
        this.conflictCode = conflictCode;
    }

    public String getConflictCode() {
        return conflictCode;
    }
}
