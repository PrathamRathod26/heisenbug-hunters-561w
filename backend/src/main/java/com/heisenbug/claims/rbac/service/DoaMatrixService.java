package com.heisenbug.claims.rbac.service;

import com.heisenbug.claims.rbac.domain.DoaMatrix;
import com.heisenbug.claims.rbac.domain.LineOfBusiness;
import com.heisenbug.claims.rbac.domain.Role;
import com.heisenbug.claims.rbac.domain.RoleName;
import com.heisenbug.claims.rbac.exception.DoaMatrixNotFoundException;
import com.heisenbug.claims.rbac.exception.DuplicateDoaMatrixException;
import com.heisenbug.claims.rbac.repository.DoaMatrixRepository;
import com.heisenbug.claims.rbac.web.dto.DoaMatrixCreateRequest;
import com.heisenbug.claims.rbac.web.dto.DoaMatrixResponse;
import com.heisenbug.claims.rbac.web.dto.DoaMatrixUpdateRequest;
import com.heisenbug.claims.rbac.web.mapper.DoaMatrixMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DoaMatrixService {

    private final DoaMatrixRepository repository;
    private final RoleService roleService;
    private final DoaMatrixMapper mapper;

    public DoaMatrixService(DoaMatrixRepository repository,
                            RoleService roleService,
                            DoaMatrixMapper mapper) {
        this.repository = repository;
        this.roleService = roleService;
        this.mapper = mapper;
    }

    @Transactional
    public DoaMatrixResponse create(DoaMatrixCreateRequest request) {
        Objects.requireNonNull(request, "request");

        if (repository.existsByRoleIdAndLineOfBusinessAndGeo(
                request.roleId(), request.lineOfBusiness(), request.geo())) {
            throw new DuplicateDoaMatrixException(
                    request.roleId(), request.lineOfBusiness(), request.geo());
        }

        Role role = roleService.getEntityOrThrow(request.roleId());
        DoaMatrix entity = new DoaMatrix(
                role,
                request.lineOfBusiness(),
                request.geo(),
                request.approveUpToPaise(),
                request.fourEyeAbovePaise());

        return mapper.toResponse(repository.save(entity));
    }

    public DoaMatrixResponse findById(UUID id) {
        return repository.findWithRoleById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new DoaMatrixNotFoundException(id));
    }

    public DoaMatrixResponse findActive(RoleName roleName, LineOfBusiness lob, String geo) {
        String descriptor = "role=%s lob=%s geo=%s".formatted(roleName, lob, geo);
        return repository.findActive(roleName, lob, geo)
                .map(mapper::toResponse)
                .orElseThrow(() -> new DoaMatrixNotFoundException(descriptor));
    }

    public List<DoaMatrixResponse> findByRole(UUID roleId) {
        return repository.findAllByRoleId(roleId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<DoaMatrixResponse> findAllActive() {
        return repository.findAllByActiveTrue().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public DoaMatrixResponse update(UUID id, DoaMatrixUpdateRequest request) {
        DoaMatrix entity = repository.findWithRoleById(id)
                .orElseThrow(() -> new DoaMatrixNotFoundException(id));

        Optional.ofNullable(request.approveUpToPaise()).ifPresent(entity::setApproveUpToPaise);
        Optional.ofNullable(request.fourEyeAbovePaise()).ifPresent(entity::setFourEyeAbovePaise);
        Optional.ofNullable(request.active()).ifPresent(entity::setActive);

        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new DoaMatrixNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
