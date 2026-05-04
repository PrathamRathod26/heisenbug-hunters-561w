package com.heisenbug.claims.rbac.repository;

import com.heisenbug.claims.rbac.domain.AssignmentStatus;
import com.heisenbug.claims.rbac.domain.UserRoleAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, UUID> {

    @EntityGraph(attributePaths = {"role", "grantedBy", "revokedBy"})
    Optional<UserRoleAssignment> findWithRoleById(UUID id);

    @EntityGraph(attributePaths = {"role", "grantedBy"})
    @Query("""
            select a from UserRoleAssignment a
            where a.user.id = :userId
              and a.status = com.heisenbug.claims.rbac.domain.AssignmentStatus.ACTIVE
            """)
    List<UserRoleAssignment> findActiveByUserId(@Param("userId") UUID userId);

    boolean existsByUserIdAndRoleIdAndStatus(UUID userId, UUID roleId, AssignmentStatus status);
}
