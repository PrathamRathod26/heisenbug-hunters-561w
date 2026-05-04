package com.heisenbug.claims.rbac.web;

import com.heisenbug.claims.rbac.domain.RoleName;
import com.heisenbug.claims.rbac.service.RoleService;
import com.heisenbug.claims.rbac.web.dto.RoleCreateRequest;
import com.heisenbug.claims.rbac.web.dto.RolePermissionUpdateRequest;
import com.heisenbug.claims.rbac.web.dto.RoleResponse;
import com.heisenbug.claims.rbac.web.dto.RoleUpdateRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rbac/roles")
@Validated
@Tag(name = "rbac-roles")
public class RoleController {

    private final RoleService service;

    public RoleController(RoleService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request,
                                               UriComponentsBuilder uriBuilder) {
        RoleResponse created = service.createRole(request);
        URI location = uriBuilder.path("/api/v1/rbac/roles/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<RoleResponse>> list(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(includeInactive ? service.findAll() : service.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<RoleResponse> getByName(@PathVariable RoleName name) {
        return ResponseEntity.ok(service.findByName(name));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RoleResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody RoleUpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PutMapping("/{id}/permissions")
    public ResponseEntity<RoleResponse> replacePermissions(
            @PathVariable UUID id,
            @Valid @RequestBody RolePermissionUpdateRequest request) {
        return ResponseEntity.ok(service.replacePermissions(id, request));
    }
}
