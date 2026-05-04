package com.heisenbug.claims.claim.client;

public record PolicyStatusResponse(
        String policyNumber,
        boolean active,
        String status
) {
}
