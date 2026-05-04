package com.heisenbug.claims.rbac.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigInteger;

public record DoaMatrixUpdateRequest(

        @DecimalMin(value = "0", message = "approveUpToPaise must be non-negative")
        @Digits(integer = 18, fraction = 0, message = "approveUpToPaise must be a whole number")
        BigInteger approveUpToPaise,

        @DecimalMin(value = "0", message = "fourEyeAbovePaise must be non-negative")
        @Digits(integer = 18, fraction = 0, message = "fourEyeAbovePaise must be a whole number")
        BigInteger fourEyeAbovePaise,

        Boolean active
) {
}
