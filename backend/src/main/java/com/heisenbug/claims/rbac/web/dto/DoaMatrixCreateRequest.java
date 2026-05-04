package com.heisenbug.claims.rbac.web.dto;

import com.heisenbug.claims.rbac.domain.LineOfBusiness;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigInteger;
import java.util.UUID;

public record DoaMatrixCreateRequest(

        @NotNull(message = "roleId is required")
        UUID roleId,

        @NotNull(message = "lineOfBusiness is required")
        LineOfBusiness lineOfBusiness,

        @NotBlank(message = "geo is required")
        @Pattern(regexp = "^[A-Z]{2}(-[A-Z0-9]{1,3})?$",
                message = "geo must be an ISO-3166-1/2 region code (e.g. 'IN' or 'IN-MH')")
        String geo,

        @NotNull(message = "approveUpToPaise is required")
        @DecimalMin(value = "0", message = "approveUpToPaise must be non-negative")
        @Digits(integer = 18, fraction = 0, message = "approveUpToPaise must be a whole number")
        BigInteger approveUpToPaise,

        @NotNull(message = "fourEyeAbovePaise is required")
        @DecimalMin(value = "0", message = "fourEyeAbovePaise must be non-negative")
        @Digits(integer = 18, fraction = 0, message = "fourEyeAbovePaise must be a whole number")
        BigInteger fourEyeAbovePaise
) {
}
