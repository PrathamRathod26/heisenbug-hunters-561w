package com.heisenbug.claims.rbac.web.mapper;

import com.heisenbug.claims.rbac.domain.DoaMatrix;
import com.heisenbug.claims.rbac.web.dto.DoaMatrixResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface DoaMatrixMapper {

    @Mapping(target = "roleId", source = "role.id")
    @Mapping(target = "roleName", source = "role.name")
    DoaMatrixResponse toResponse(DoaMatrix matrix);
}
