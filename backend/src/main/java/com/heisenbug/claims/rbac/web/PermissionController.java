package com.heisenbug.claims.rbac.web;

import com.heisenbug.claims.rbac.domain.CapabilityCode;
import com.heisenbug.claims.rbac.service.PermissionService;
import com.heisenbug.claims.rbac.web.dto.PermissionResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rbac/permissions")
@Tag(name = "rbac-permissions")
public class PermissionController {

    private final PermissionService service;

    public PermissionController(PermissionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<PermissionResponse>> list(
            @RequestParam(required = false) CapabilityCode capability) {
        List<PermissionResponse> body = (capability == null)
                ? service.findAll()
                : service.findByCapability(capability);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PermissionResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
