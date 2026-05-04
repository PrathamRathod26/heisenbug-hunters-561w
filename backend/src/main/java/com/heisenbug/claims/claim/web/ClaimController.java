package com.heisenbug.claims.claim.web;

import com.heisenbug.claims.claim.client.MlAnalyzeResponse;
import com.heisenbug.claims.claim.config.ClaimsProperties;
import com.heisenbug.claims.claim.domain.ClaimStatus;
import com.heisenbug.claims.claim.service.ClaimService;
import com.heisenbug.claims.claim.web.dto.ClaimCreateRequest;
import com.heisenbug.claims.claim.web.dto.ClaimResponse;
import com.heisenbug.claims.claim.web.dto.ClaimSummaryResponse;
import com.heisenbug.claims.claim.web.dto.ClaimWithAnalysisResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/claims")
@Validated
@Tag(name = "claims")
public class ClaimController {

    private final ClaimService service;
    private final ClaimsProperties claimsProperties;

    public ClaimController(ClaimService service, ClaimsProperties claimsProperties) {
        this.service = service;
        this.claimsProperties = claimsProperties;
    }

    @Operation(
            summary = "Create a claim (JSON)",
            description = "Create a FNOL claim record without invoking the AI pipeline. Use `/with-analysis` " +
                    "for the multipart path that persists evidence files and runs FR-3/FR-4/FR-7."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Claim created"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED — body didn't satisfy DTO constraints")
    })
    @PostMapping
    public ResponseEntity<ClaimResponse> create(@Valid @RequestBody ClaimCreateRequest request,
                                                UriComponentsBuilder uriBuilder) {
        ClaimResponse created = service.createClaim(request);
        URI location = uriBuilder.path("/api/v1/claims/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(
            summary = "Fetch a single claim by id",
            description = "Returns the full Claim aggregate including evidences (photos + optional video). " +
                    "Lazy-loaded associations are pre-fetched via `@EntityGraph` on the repository."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim returned"),
            @ApiResponse(responseCode = "404", description = "CLAIM_NOT_FOUND — no claim with that id")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ClaimResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findClaim(id));
    }

    @Operation(
            summary = "Get stored AI analysis for a claim",
            description = "Returns the persisted `MlAnalyzeResponse` for the claim — model version, detections " +
                    "per image, cost estimate (paise), surveyor assessment. Persisted at claim-creation time " +
                    "in the `claim_analysis` table (FRS AC-2.1.13, TSD ADR-027). Returns 204 when the claim " +
                    "has no stored analysis (either created pre-v1.3, or ML-service was unavailable during " +
                    "submission and its fallback response wasn't retained)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analysis returned"),
            @ApiResponse(responseCode = "204", description = "Claim exists but has no stored analysis"),
            @ApiResponse(responseCode = "404", description = "CLAIM_NOT_FOUND — no claim with that id")
    })
    @GetMapping("/{id}/analysis")
    public ResponseEntity<MlAnalyzeResponse> getAnalysis(@PathVariable UUID id) {
        return service.findAnalysisForClaim(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(
            summary = "Stream an evidence file (photo / video) by id",
            description = "Streams the uploaded bytes with the stored `Content-Type` and " +
                    "`Content-Disposition: inline; filename=…`. Phase-1 backing store is the local filesystem " +
                    "at `{claims.upload-dir}/{evidenceId}` (TSD ADR-026); phase-2 migrates to S3 (ADR-005) " +
                    "with the same HTTP contract. Used by the claim-detail UI for thumbnails and the " +
                    "zoomable viewer dialog (FRS AC-2.1.12).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Evidence bytes",
                            content = @Content(mediaType = "application/octet-stream",
                                    schema = @Schema(type = "string", format = "binary"))),
                    @ApiResponse(responseCode = "404",
                            description = "No such evidence, or the byte stream is missing from the backing store")
            }
    )
    @GetMapping("/{claimId}/evidence/{evidenceId}")
    public ResponseEntity<byte[]> getEvidenceContent(@PathVariable UUID claimId,
                                                     @PathVariable UUID evidenceId) {
        return service.loadEvidenceContent(claimId, evidenceId)
                .map(c -> {
                    MediaType ct = (c.contentType() != null)
                            ? MediaType.parseMediaType(c.contentType())
                            : MediaType.APPLICATION_OCTET_STREAM;
                    return ResponseEntity.ok()
                            .contentType(ct)
                            .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                    "inline; filename=\"" + (c.filename() == null ? "evidence" : c.filename()) + "\"")
                            .body(c.bytes());
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "List claims filtered by status, paginated",
            description = "Filters by `status` query parameter. Paging via standard Spring Data `Pageable` " +
                    "(default size 25, sort by createdAt). `@EntityGraph` on the repository avoids N+1 on evidences."
    )
    @GetMapping
    public ResponseEntity<Page<ClaimSummaryResponse>> list(
            @Parameter(description = "Status to filter by (required)", required = true)
            @RequestParam @NotNull(message = "status query parameter is required") ClaimStatus status,
            @PageableDefault(size = 25, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(service.listSummaries(status, pageable));
    }

    @Operation(
            summary = "Claims-by-status histogram for the dashboard",
            description = "Aggregated counts keyed by ClaimStatus. Powers the Claims-by-status doughnut on the dashboard."
    )
    @GetMapping("/stats/status-counts")
    public ResponseEntity<Map<ClaimStatus, Long>> statusCounts() {
        return ResponseEntity.ok(service.statusCounts());
    }

    @Operation(
            summary = "Update a claim's status",
            description = "State-machine transition (FR-9). The frontend hides this control from the Policyholder " +
                    "persona per FRS §3.3; the API-layer RBAC check will reject calls from principals without the " +
                    "required §3.2 capability once auth is wired."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated; body is the updated ClaimResponse"),
            @ApiResponse(responseCode = "404", description = "CLAIM_NOT_FOUND")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ClaimResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam @NotNull(message = "newStatus is required") ClaimStatus newStatus) {
        ClaimResponse updated = service.updateStatus(id, newStatus);
        return ResponseEntity.status(HttpStatus.OK).body(updated);
    }

    @Operation(
            summary = "Claims subsystem config for the SPA",
            description = "Advertises the configured photo-upload limits so the frontend doesn't hardcode them. " +
                    "Changing `claims.max-evidences` / `claims.min-evidences-for-analysis` in the backend " +
                    "properties takes effect on the next SPA page-load with no frontend rebuild (FRS AC-2.1.10)."
    )
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> config() {
        return ResponseEntity.ok(Map.of(
                "maxEvidences", claimsProperties.getMaxEvidences(),
                "minEvidencesForAnalysis", claimsProperties.getMinEvidencesForAnalysis()
        ));
    }

    @Operation(
            summary = "Create a claim with synchronous AI analysis (multipart)",
            description = "Combined FNOL + AI pipeline entry point. Accepts scalar form fields and an `images[]` " +
                    "list plus optional `video`. Backend orchestrates: ML analyze (MlAnalyzeClient → " +
                    "POST ml-services:/api/v1/analyze) → persist Claim → write evidence bytes to " +
                    "`{claims.upload-dir}/{evidenceId}` → persist ClaimAnalysis. Fails closed on ML service " +
                    "unavailability: the claim is still created with status=FNOL_RECEIVED and the returned " +
                    "analysis has `modelVersion: \"ml-service-unavailable\"`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Claim created; body is ClaimWithAnalysisResponse"),
            @ApiResponse(responseCode = "400", description = "Validation error on form fields, too many/few images, or malformed multipart body")
    })
    @PostMapping(value = "/with-analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClaimWithAnalysisResponse> createWithAnalysis(
            @RequestParam
            @NotBlank(message = "policyNumber is required")
            @Pattern(regexp = "^[A-Z0-9-]{6,32}$",
                    message = "policyNumber must be 6-32 chars of A-Z, 0-9 or '-'") String policyNumber,
            @RequestParam
            @NotBlank(message = "claimantName is required")
            @Size(min = 2, max = 120, message = "claimantName must be 2-120 characters") String claimantName,
            @RequestParam
            @NotNull(message = "estimatedLoss is required")
            @DecimalMin(value = "0.00", inclusive = true,
                    message = "estimatedLoss must be non-negative")
            @Digits(integer = 12, fraction = 2,
                    message = "estimatedLoss must have at most 12 digits and 2 decimals") BigDecimal estimatedLoss,
            @RequestParam(required = false)
            @Size(max = 4000, message = "narrative must be at most 4000 characters") String narrative,
            @RequestParam(required = false)
            @Size(max = 32, message = "incidentTime must be at most 32 characters") String incidentTime,
            @RequestPart("images") List<MultipartFile> images,
            @RequestPart(value = "video", required = false) MultipartFile video,
            UriComponentsBuilder uriBuilder) {

        int max = claimsProperties.getMaxEvidences();
        if (images != null && images.size() > max) {
            throw new IllegalArgumentException(
                    "at most %d images are allowed (got %d); adjust `claims.max-evidences` to change this limit"
                            .formatted(max, images.size()));
        }

        ClaimCreateRequest req = new ClaimCreateRequest(policyNumber, claimantName, estimatedLoss, null);
        ClaimWithAnalysisResponse body = service.createWithAnalysis(req, images, video, narrative, incidentTime);

        URI location = uriBuilder.path("/api/v1/claims/{id}")
                .buildAndExpand(body.claim().id())
                .toUri();
        return ResponseEntity.created(location).body(body);
    }
}
