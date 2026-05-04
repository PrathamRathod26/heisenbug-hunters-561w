package com.heisenbug.claims.common;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        List<FieldIssue> fieldErrors,
        Instant timestamp
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of(), Instant.now());
    }

    public static ErrorResponse of(String code, String message, List<FieldIssue> fieldErrors) {
        return new ErrorResponse(code, message, fieldErrors, Instant.now());
    }

    public record FieldIssue(String field, String message) {
    }
}
