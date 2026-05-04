package com.heisenbug.claims.rbac.web;

import com.heisenbug.claims.rbac.domain.RoleName;
import com.heisenbug.claims.rbac.domain.UserStatus;
import com.heisenbug.claims.rbac.domain.UserType;
import com.heisenbug.claims.rbac.service.UserService;
import com.heisenbug.claims.rbac.web.dto.EffectivePermissionsResponse;
import com.heisenbug.claims.rbac.web.dto.UserCreateRequest;
import com.heisenbug.claims.rbac.web.dto.UserResponse;
import com.heisenbug.claims.rbac.web.dto.UserRoleAssignRequest;
import com.heisenbug.claims.rbac.web.dto.UserRoleAssignmentResponse;
import com.heisenbug.claims.rbac.web.dto.UserSummaryResponse;
import com.heisenbug.claims.rbac.web.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rbac/users")
@Validated
@Tag(name = "rbac-users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request,
                                               UriComponentsBuilder uriBuilder) {
        UserResponse created = service.createUser(request);
        URI location = uriBuilder.path("/api/v1/rbac/users/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<UserSummaryResponse>> list(
            @RequestParam(required = false) UserType type,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 25, sort = "email") Pageable pageable) {
        return ResponseEntity.ok(service.list(type, status, pageable));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PostMapping("/{userId}/role-assignments")
    public ResponseEntity<UserRoleAssignmentResponse> assignRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UserRoleAssignRequest request) {
        return ResponseEntity.ok(service.assignRole(userId, request));
    }

    @GetMapping("/{userId}/role-assignments")
    public ResponseEntity<List<UserRoleAssignmentResponse>> listActiveAssignments(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(service.listActiveAssignments(userId));
    }

    @DeleteMapping("/role-assignments/{assignmentId}")
    public ResponseEntity<UserRoleAssignmentResponse> revokeRole(
            @PathVariable UUID assignmentId,
            @RequestParam(required = false) UUID revokedBy) {
        return ResponseEntity.ok(service.revokeRole(assignmentId, revokedBy));
    }

    @GetMapping("/{userId}/effective-permissions")
    public ResponseEntity<EffectivePermissionsResponse> getEffectivePermissions(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(service.getEffectivePermissions(userId));
    }

    @GetMapping("/stats/active-users-per-role")
    public ResponseEntity<Map<RoleName, Long>> activeUsersPerRole() {
        return ResponseEntity.ok(service.activeUsersPerRole());
    }
}
