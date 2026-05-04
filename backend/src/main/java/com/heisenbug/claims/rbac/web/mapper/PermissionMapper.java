package com.heisenbug.claims.rbac.web.mapper;

import com.heisenbug.claims.rbac.domain.Permission;
import com.heisenbug.claims.rbac.web.dto.PermissionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.Set;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PermissionMapper {

    PermissionResponse toResponse(Permission permission);

    Set<PermissionResponse> toResponses(Set<Permission> permissions);
}
