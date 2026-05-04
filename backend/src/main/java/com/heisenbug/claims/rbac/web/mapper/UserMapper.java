package com.heisenbug.claims.rbac.web.mapper;

import com.heisenbug.claims.rbac.domain.User;
import com.heisenbug.claims.rbac.domain.UserRoleAssignment;
import com.heisenbug.claims.rbac.web.dto.UserCreateRequest;
import com.heisenbug.claims.rbac.web.dto.UserResponse;
import com.heisenbug.claims.rbac.web.dto.UserRoleAssignmentResponse;
import com.heisenbug.claims.rbac.web.dto.UserSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "roleAssignments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    User toEntity(UserCreateRequest request);

    @Mapping(target = "roleAssignments", source = "roleAssignments")
    UserResponse toResponse(User user);

    UserSummaryResponse toSummary(User user);

    List<UserRoleAssignmentResponse> toAssignmentResponses(List<UserRoleAssignment> assignments);

    @Mapping(target = "roleId", source = "role.id")
    @Mapping(target = "roleName", source = "role.name")
    @Mapping(target = "grantedByEmail", source = "grantedBy", qualifiedByName = "userEmail")
    @Mapping(target = "revokedByEmail", source = "revokedBy", qualifiedByName = "userEmail")
    UserRoleAssignmentResponse toAssignmentResponse(UserRoleAssignment assignment);

    @Named("userEmail")
    default String userEmail(User user) {
        return Optional.ofNullable(user).map(User::getEmail).orElse(null);
    }
}
