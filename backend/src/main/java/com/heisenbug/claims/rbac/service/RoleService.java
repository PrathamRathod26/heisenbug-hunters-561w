package com.heisenbug.claims.rbac.service;

import com.heisenbug.claims.rbac.domain.Permission;
import com.heisenbug.claims.rbac.domain.Role;
import com.heisenbug.claims.rbac.domain.RoleName;
import com.heisenbug.claims.rbac.exception.DuplicateRoleException;
import com.heisenbug.claims.rbac.exception.RoleNotFoundException;
import com.heisenbug.claims.rbac.repository.RoleRepository;
import com.heisenbug.claims.rbac.web.dto.RoleCreateRequest;
import com.heisenbug.claims.rbac.web.dto.RolePermissionUpdateRequest;
import com.heisenbug.claims.rbac.web.dto.RoleResponse;
import com.heisenbug.claims.rbac.web.dto.RoleUpdateRequest;
import com.heisenbug.claims.rbac.web.mapper.RoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository repository;
    private final PermissionService permissionService;
    private final RoleMapper mapper;

    public RoleService(RoleRepository repository,
                       PermissionService permissionService,
                       RoleMapper mapper) {
        this.repository = repository;
        this.permissionService = permissionService;
        this.mapper = mapper;
    }

    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        Objects.requireNonNull(request, "request");

        if (repository.existsByName(request.name())) {
            throw new DuplicateRoleException(request.name());
        }

        Role role = mapper.toEntity(request);
        Set<Permission> permissions = permissionService.resolveByIds(
                Optional.ofNullable(request.permissionIds()).orElseGet(Set::of));
        role.setPermissions(permissions);

        Role saved = repository.save(role);
        return mapper.toResponse(saved);
    }

    public RoleResponse findById(UUID id) {
        return repository.findWithPermissionsById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new RoleNotFoundException(id));
    }

    public RoleResponse findByName(RoleName name) {
        return repository.findWithPermissionsByName(name)
                .map(mapper::toResponse)
                .orElseThrow(() -> new RoleNotFoundException(name.name()));
    }

    public List<RoleResponse> findAllActive() {
        return repository.findAllByActiveTrue().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<RoleResponse> findAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public RoleResponse update(UUID id, RoleUpdateRequest request) {
        Role role = repository.findWithPermissionsById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));

        Optional.ofNullable(request.description()).ifPresent(role::setDescription);
        Optional.ofNullable(request.active()).ifPresent(role::setActive);

        return mapper.toResponse(role);
    }

    @Transactional
    public RoleResponse replacePermissions(UUID roleId, RolePermissionUpdateRequest request) {
        Role role = repository.findWithPermissionsById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));

        Set<Permission> permissions = permissionService.resolveByIds(request.permissionIds());
        role.setPermissions(permissions);

        return mapper.toResponse(role);
    }

    Role getEntityOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
    }
}
