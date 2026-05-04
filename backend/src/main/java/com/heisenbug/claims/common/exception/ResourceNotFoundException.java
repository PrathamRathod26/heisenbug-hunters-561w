package com.heisenbug.claims.common.exception;

public abstract class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String identifier;

    protected ResourceNotFoundException(String resourceType, String identifier) {
        super("%s not found: %s".formatted(resourceType, identifier));
        this.resourceType = resourceType;
        this.identifier = identifier;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getIdentifier() {
        return identifier;
    }
}
