package com.heisenbug.claims.rbac.repository;

import com.heisenbug.claims.rbac.domain.User;
import com.heisenbug.claims.rbac.domain.UserStatus;
import com.heisenbug.claims.rbac.domain.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"roleAssignments", "roleAssignments.role"})
    Optional<User> findWithAssignmentsById(UUID id);

    @EntityGraph(attributePaths = {"roleAssignments", "roleAssignments.role", "roleAssignments.role.permissions"})
    @Query("select distinct u from User u where u.id = :id")
    Optional<User> findWithEffectivePermissionsById(@Param("id") UUID id);

    Optional<User> findByEmail(String email);

    Optional<User> findByExternalId(String externalId);

    Page<User> findAllByUserType(UserType userType, Pageable pageable);

    Page<User> findAllByStatus(UserStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"roleAssignments", "roleAssignments.role"})
    @Query("""
            select distinct u from User u
            join u.roleAssignments a
            where a.role.id = :roleId
              and a.status = com.heisenbug.claims.rbac.domain.AssignmentStatus.ACTIVE
            """)
    List<User> findActiveUsersByRoleId(@Param("roleId") UUID roleId);

    boolean existsByEmail(String email);

    boolean existsByExternalId(String externalId);
}
