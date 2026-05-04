package com.heisenbug.claims.rbac.repository;

import com.heisenbug.claims.rbac.domain.CapabilityCode;
import com.heisenbug.claims.rbac.domain.Permission;
import com.heisenbug.claims.rbac.domain.PermissionScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCapabilityAndScope(CapabilityCode capability, PermissionScope scope);

    List<Permission> findAllByCapability(CapabilityCode capability);

    boolean existsByCapabilityAndScope(CapabilityCode capability, PermissionScope scope);
}
