package com.heisenbug.claims.rbac.web.mapper;

import com.heisenbug.claims.rbac.domain.Role;
import com.heisenbug.claims.rbac.web.dto.RoleCreateRequest;
import com.heisenbug.claims.rbac.web.dto.RoleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        uses = PermissionMapper.class,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface RoleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Role toEntity(RoleCreateRequest request);

    RoleResponse toResponse(Role role);
}
