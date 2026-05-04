package com.heisenbug.claims.rbac.service;

import com.heisenbug.claims.rbac.domain.CapabilityCode;
import com.heisenbug.claims.rbac.domain.Permission;
import com.heisenbug.claims.rbac.exception.PermissionNotFoundException;
import com.heisenbug.claims.rbac.repository.PermissionRepository;
import com.heisenbug.claims.rbac.web.dto.PermissionResponse;
import com.heisenbug.claims.rbac.web.mapper.PermissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PermissionService {

    private final PermissionRepository repository;
    private final PermissionMapper mapper;

    public PermissionService(PermissionRepository repository, PermissionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public PermissionResponse findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new PermissionNotFoundException(id));
    }

    public List<PermissionResponse> findAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<PermissionResponse> findByCapability(CapabilityCode capability) {
        return repository.findAllByCapability(capability).stream()
                .map(mapper::toResponse)
                .toList();
    }

    Set<Permission> resolveByIds(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        Set<Permission> found = repository.findAllById(ids).stream()
                .collect(Collectors.toUnmodifiableSet());

        Set<UUID> foundIds = found.stream()
                .map(Permission::getId)
                .collect(Collectors.toSet());

        ids.stream()
                .filter(id -> !foundIds.contains(id))
                .findFirst()
                .ifPresent(missing -> {
                    throw new PermissionNotFoundException(missing);
                });

        return found;
    }
}
