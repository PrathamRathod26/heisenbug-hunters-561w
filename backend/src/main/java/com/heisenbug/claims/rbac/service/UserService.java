package com.heisenbug.claims.rbac.service;

import com.heisenbug.claims.rbac.domain.AssignmentStatus;
import com.heisenbug.claims.rbac.domain.CapabilityCode;
import com.heisenbug.claims.rbac.domain.Permission;
import com.heisenbug.claims.rbac.domain.PermissionScope;
import com.heisenbug.claims.rbac.domain.Role;
import com.heisenbug.claims.rbac.domain.RoleName;
import com.heisenbug.claims.rbac.domain.User;
import com.heisenbug.claims.rbac.domain.UserRoleAssignment;
import com.heisenbug.claims.rbac.domain.UserStatus;
import com.heisenbug.claims.rbac.domain.UserType;
import com.heisenbug.claims.rbac.exception.DuplicateRoleAssignmentException;
import com.heisenbug.claims.rbac.exception.DuplicateUserException;
import com.heisenbug.claims.rbac.exception.RoleAssignmentNotFoundException;
import com.heisenbug.claims.rbac.exception.UserNotFoundException;
import com.heisenbug.claims.rbac.repository.UserRepository;
import com.heisenbug.claims.rbac.repository.UserRoleAssignmentRepository;
import com.heisenbug.claims.rbac.web.dto.EffectivePermissionsResponse;
import com.heisenbug.claims.rbac.web.dto.EffectivePermissionsResponse.CapabilityPermission;
import com.heisenbug.claims.rbac.web.dto.UserCreateRequest;
import com.heisenbug.claims.rbac.web.dto.UserResponse;
import com.heisenbug.claims.rbac.web.dto.UserRoleAssignRequest;
import com.heisenbug.claims.rbac.web.dto.UserRoleAssignmentResponse;
import com.heisenbug.claims.rbac.web.dto.UserSummaryResponse;
import com.heisenbug.claims.rbac.web.dto.UserUpdateRequest;
import com.heisenbug.claims.rbac.web.mapper.UserMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Map<PermissionScope, Integer> SCOPE_RANK = new EnumMap<>(Map.of(
            PermissionScope.READ_ONLY, 0,
            PermissionScope.FOUR_EYE_REQUIRED, 1,
            PermissionScope.ASSIGNMENT_RESTRICTED, 2,
            PermissionScope.GRANTED, 3
    ));

    private final UserRepository repository;
    private final UserRoleAssignmentRepository assignmentRepository;
    private final RoleService roleService;
    private final UserMapper mapper;

    public UserService(UserRepository repository,
                       UserRoleAssignmentRepository assignmentRepository,
                       RoleService roleService,
                       UserMapper mapper) {
        this.repository = repository;
        this.assignmentRepository = assignmentRepository;
        this.roleService = roleService;
        this.mapper = mapper;
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        Objects.requireNonNull(request, "request");

        if (repository.existsByEmail(request.email())) {
            throw new DuplicateUserException("email", request.email());
        }
        if (repository.existsByExternalId(request.externalId())) {
            throw new DuplicateUserException("externalId", request.externalId());
        }

        User entity = mapper.toEntity(request);
        Optional.ofNullable(request.licenceId()).ifPresent(entity::setLicenceId);
        Optional.ofNullable(request.licenceExpiresAt()).ifPresent(entity::setLicenceExpiresAt);

        User saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    public UserResponse findById(UUID id) {
        return repository.findWithAssignmentsById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public Page<UserSummaryResponse> list(UserType type, UserStatus status, Pageable pageable) {
        Page<User> page = (type != null)
                ? repository.findAllByUserType(type, pageable)
                : (status != null)
                        ? repository.findAllByStatus(status, pageable)
                        : repository.findAll(pageable);
        return page.map(mapper::toSummary);
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        User user = repository.findWithAssignmentsById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        Optional.ofNullable(request.displayName()).ifPresent(user::setDisplayName);
        Optional.ofNullable(request.status()).ifPresent(user::setStatus);
        Optional.ofNullable(request.licenceId()).ifPresent(user::setLicenceId);
        Optional.ofNullable(request.licenceExpiresAt()).ifPresent(user::setLicenceExpiresAt);

        return mapper.toResponse(user);
    }

    @Transactional
    public UserRoleAssignmentResponse assignRole(UUID userId, UserRoleAssignRequest request) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (assignmentRepository.existsByUserIdAndRoleIdAndStatus(
                userId, request.roleId(), AssignmentStatus.ACTIVE)) {
            throw new DuplicateRoleAssignmentException(userId, request.roleId());
        }

        Role role = roleService.getEntityOrThrow(request.roleId());
        User grantedBy = Optional.ofNullable(request.grantedByUserId())
                .flatMap(repository::findById)
                .orElse(null);

        UserRoleAssignment assignment = new UserRoleAssignment(user, role, grantedBy, request.note());
        user.addRoleAssignment(assignment);

        UserRoleAssignment saved = assignmentRepository.save(assignment);
        return mapper.toAssignmentResponse(saved);
    }

    @Transactional
    public UserRoleAssignmentResponse revokeRole(UUID assignmentId, UUID revokedByUserId) {
        UserRoleAssignment assignment = assignmentRepository.findWithRoleById(assignmentId)
                .orElseThrow(() -> new RoleAssignmentNotFoundException(assignmentId));

        User revokedBy = Optional.ofNullable(revokedByUserId)
                .flatMap(repository::findById)
                .orElse(null);

        assignment.revoke(revokedBy);
        return mapper.toAssignmentResponse(assignment);
    }

    public List<UserRoleAssignmentResponse> listActiveAssignments(UUID userId) {
        if (!repository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return assignmentRepository.findActiveByUserId(userId).stream()
                .map(mapper::toAssignmentResponse)
                .toList();
    }

    public EffectivePermissionsResponse getEffectivePermissions(UUID userId) {
        User user = repository.findWithEffectivePermissionsById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<UserRoleAssignment> activeAssignments = user.getRoleAssignments().stream()
                .filter(a -> a.getStatus() == AssignmentStatus.ACTIVE)
                .filter(a -> a.getRole() != null && a.getRole().isActive())
                .toList();

        Set<RoleName> activeRoles = activeAssignments.stream()
                .map(a -> a.getRole().getName())
                .collect(Collectors.toCollection(HashSet::new));

        record RolePermission(RoleName role, CapabilityCode capability, PermissionScope scope) {
        }

        List<RolePermission> rolePerms = activeAssignments.stream()
                .flatMap(a -> a.getRole().getPermissions().stream()
                        .map(p -> new RolePermission(
                                a.getRole().getName(),
                                p.getCapability(),
                                p.getScope())))
                .toList();

        Map<CapabilityCode, List<RolePermission>> byCapability = rolePerms.stream()
                .collect(Collectors.groupingBy(RolePermission::capability));

        Set<CapabilityPermission> permissions = byCapability.entrySet().stream()
                .map(entry -> {
                    PermissionScope strongest = entry.getValue().stream()
                            .map(RolePermission::scope)
                            .max(java.util.Comparator.comparingInt(SCOPE_RANK::get))
                            .orElse(PermissionScope.READ_ONLY);

                    Set<RoleName> grantedBy = entry.getValue().stream()
                            .map(RolePermission::role)
                            .collect(Collectors.toCollection(HashSet::new));

                    return new CapabilityPermission(entry.getKey(), strongest, grantedBy);
                })
                .collect(Collectors.toSet());

        return new EffectivePermissionsResponse(userId, activeRoles, permissions);
    }

    public Map<RoleName, Long> activeUsersPerRole() {
        return repository.findAll().stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .flatMap(u -> u.getRoleAssignments().stream()
                        .filter(a -> a.getStatus() == AssignmentStatus.ACTIVE)
                        .map(a -> a.getRole().getName()))
                .collect(Collectors.groupingBy(
                        rn -> rn,
                        () -> new EnumMap<>(RoleName.class),
                        Collectors.counting()));
    }

    @SuppressWarnings("unused")
    User getEntityOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @SuppressWarnings("unused")
    private static boolean isPermissionGranted(Permission p) {
        return p.getScope() != PermissionScope.READ_ONLY;
    }
}
