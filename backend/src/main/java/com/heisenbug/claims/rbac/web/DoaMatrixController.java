package com.heisenbug.claims.rbac.web;

import com.heisenbug.claims.rbac.domain.LineOfBusiness;
import com.heisenbug.claims.rbac.domain.RoleName;
import com.heisenbug.claims.rbac.service.DoaMatrixService;
import com.heisenbug.claims.rbac.web.dto.DoaMatrixCreateRequest;
import com.heisenbug.claims.rbac.web.dto.DoaMatrixResponse;
import com.heisenbug.claims.rbac.web.dto.DoaMatrixUpdateRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rbac/doa-matrix")
@Validated
@Tag(name = "rbac-doa-matrix")
public class DoaMatrixController {

    private final DoaMatrixService service;

    public DoaMatrixController(DoaMatrixService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DoaMatrixResponse> create(@Valid @RequestBody DoaMatrixCreateRequest request,
                                                    UriComponentsBuilder uriBuilder) {
        DoaMatrixResponse created = service.create(request);
        URI location = uriBuilder.path("/api/v1/rbac/doa-matrix/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoaMatrixResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/lookup")
    public ResponseEntity<DoaMatrixResponse> lookup(
            @RequestParam @NotNull(message = "role is required") RoleName role,
            @RequestParam @NotNull(message = "lob is required") LineOfBusiness lob,
            @RequestParam @NotBlank(message = "geo is required") String geo) {
        return ResponseEntity.ok(service.findActive(role, lob, geo));
    }

    @GetMapping
    public ResponseEntity<List<DoaMatrixResponse>> list(
            @RequestParam(required = false) UUID roleId) {
        List<DoaMatrixResponse> body = (roleId != null)
                ? service.findByRole(roleId)
                : service.findAllActive();
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DoaMatrixResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody DoaMatrixUpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
