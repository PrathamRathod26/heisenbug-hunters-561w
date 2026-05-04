package com.heisenbug.claims.rbac.web.dto;

import jakarta.validation.constraints.Size;

public record RoleUpdateRequest(

        @Size(max = 255, message = "description must be at most 255 characters")
        String description,

        Boolean active
) {
}
