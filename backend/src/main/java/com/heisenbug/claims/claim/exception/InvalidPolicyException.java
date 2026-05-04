package com.heisenbug.claims.claim.exception;

public class InvalidPolicyException extends RuntimeException {

    private final String policyNumber;

    public InvalidPolicyException(String policyNumber, String reason) {
        super("Policy " + policyNumber + " is invalid: " + reason);
        this.policyNumber = policyNumber;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }
}
