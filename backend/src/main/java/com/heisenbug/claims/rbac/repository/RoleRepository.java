package com.heisenbug.claims.rbac.repository;

import com.heisenbug.claims.rbac.domain.Role;
import com.heisenbug.claims.rbac.domain.RoleName;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsById(UUID id);

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsByName(RoleName name);

    Optional<Role> findByName(RoleName name);

    @EntityGraph(attributePaths = "permissions")
    List<Role> findAllByActiveTrue();

    boolean existsByName(RoleName name);
}
