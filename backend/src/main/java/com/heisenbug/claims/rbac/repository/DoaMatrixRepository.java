package com.heisenbug.claims.rbac.repository;

import com.heisenbug.claims.rbac.domain.DoaMatrix;
import com.heisenbug.claims.rbac.domain.LineOfBusiness;
import com.heisenbug.claims.rbac.domain.RoleName;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoaMatrixRepository extends JpaRepository<DoaMatrix, UUID> {

    @EntityGraph(attributePaths = "role")
    Optional<DoaMatrix> findWithRoleById(UUID id);

    @EntityGraph(attributePaths = "role")
    @Query("""
            select m from DoaMatrix m
            where m.role.name = :roleName
              and m.lineOfBusiness = :lob
              and m.geo = :geo
              and m.active = true
            """)
    Optional<DoaMatrix> findActive(@Param("roleName") RoleName roleName,
                                   @Param("lob") LineOfBusiness lob,
                                   @Param("geo") String geo);

    @EntityGraph(attributePaths = "role")
    List<DoaMatrix> findAllByRoleId(UUID roleId);

    @EntityGraph(attributePaths = "role")
    List<DoaMatrix> findAllByActiveTrue();

    boolean existsByRoleIdAndLineOfBusinessAndGeo(UUID roleId, LineOfBusiness lob, String geo);
}
