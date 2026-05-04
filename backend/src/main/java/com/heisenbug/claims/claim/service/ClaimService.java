package com.heisenbug.claims.claim.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heisenbug.claims.claim.client.MlAnalyzeClient;
import com.heisenbug.claims.claim.client.MlAnalyzeResponse;
import com.heisenbug.claims.claim.config.ClaimsProperties;
import com.heisenbug.claims.claim.domain.Claim;
import com.heisenbug.claims.claim.domain.ClaimAnalysis;
import com.heisenbug.claims.claim.domain.ClaimEvidence;
import com.heisenbug.claims.claim.domain.ClaimStatus;
import com.heisenbug.claims.claim.domain.EvidenceType;
import com.heisenbug.claims.claim.exception.ClaimNotFoundException;
import com.heisenbug.claims.claim.repository.ClaimAnalysisRepository;
import com.heisenbug.claims.claim.repository.ClaimEvidenceRepository;
import com.heisenbug.claims.claim.repository.ClaimRepository;
import com.heisenbug.claims.claim.web.dto.ClaimCreateRequest;
import com.heisenbug.claims.claim.web.dto.ClaimResponse;
import com.heisenbug.claims.claim.web.dto.ClaimSummaryResponse;
import com.heisenbug.claims.claim.web.dto.ClaimWithAnalysisResponse;
import com.heisenbug.claims.claim.web.mapper.ClaimMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ClaimService {

    private static final Logger log = LoggerFactory.getLogger(ClaimService.class);

    private final ClaimRepository repository;
    private final ClaimAnalysisRepository analysisRepository;
    private final ClaimEvidenceRepository evidenceRepository;
    private final ClaimMapper mapper;
    private final MlAnalyzeClient mlClient;
    private final ClaimsProperties claimsProperties;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public ClaimService(ClaimRepository repository,
                        ClaimAnalysisRepository analysisRepository,
                        ClaimEvidenceRepository evidenceRepository,
                        ClaimMapper mapper,
                        MlAnalyzeClient mlClient,
                        ClaimsProperties claimsProperties) {
        this.repository = repository;
        this.analysisRepository = analysisRepository;
        this.evidenceRepository = evidenceRepository;
        this.mapper = mapper;
        this.mlClient = mlClient;
        this.claimsProperties = claimsProperties;
    }

    public record EvidenceContent(byte[] bytes, String contentType, String filename) {}

    @Transactional
    public ClaimResponse createClaim(ClaimCreateRequest request) {
        Objects.requireNonNull(request, "request");

        Claim entity = mapper.toEntity(request);
        entity.setStatus(ClaimStatus.FNOL_RECEIVED);
        if (entity.getEvidences() == null) {
            entity.setEvidences(new ArrayList<>());
        }
        Claim saved = repository.save(entity);

        log.info("claim created id={} policy={} evidences={}",
                saved.getId(), saved.getPolicyNumber(), saved.getEvidences().size());
        return mapper.toResponse(saved);
    }

    public ClaimResponse findClaim(UUID id) {
        return repository.findWithEvidencesById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ClaimNotFoundException(id));
    }

    public Optional<MlAnalyzeResponse> findAnalysisForClaim(UUID claimId) {
        if (!repository.existsById(claimId)) {
            throw new ClaimNotFoundException(claimId);
        }
        return analysisRepository.findByClaimId(claimId)
                .map(this::deserializePayload);
    }

    public Optional<EvidenceContent> loadEvidenceContent(UUID claimId, UUID evidenceId) {
        ClaimEvidence ev = evidenceRepository.findById(evidenceId).orElse(null);
        if (ev == null) return Optional.empty();
        if (ev.getClaim() == null || !ev.getClaim().getId().equals(claimId)) {
            return Optional.empty();
        }
        Path path = Paths.get(claimsProperties.getUploadDir(), evidenceId.toString());
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            return Optional.of(new EvidenceContent(bytes, ev.getContentType(), ev.getUri()));
        } catch (IOException e) {
            log.error("failed to read evidence {} at {}", evidenceId, path.toAbsolutePath(), e);
            return Optional.empty();
        }
    }

    public List<ClaimResponse> findAllByStatus(ClaimStatus status) {
        return repository.findAllWithEvidencesByStatus(status).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public Page<ClaimSummaryResponse> listSummaries(ClaimStatus status, Pageable pageable) {
        return repository.findAllByStatus(status, pageable)
                .map(mapper::toSummary);
    }

    public Map<ClaimStatus, Long> statusCounts() {
        return java.util.Arrays.stream(ClaimStatus.values())
                .collect(java.util.stream.Collectors.toMap(
                        s -> s,
                        repository::countByStatus,
                        (a, b) -> a,
                        () -> new EnumMap<>(ClaimStatus.class)));
    }

    @Transactional
    public ClaimWithAnalysisResponse createWithAnalysis(ClaimCreateRequest request,
                                                        List<MultipartFile> images,
                                                        MultipartFile video,
                                                        String narrative,
                                                        String incidentTime) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(images, "images");

        int max = claimsProperties.getMaxEvidences();
        int min = claimsProperties.getMinEvidencesForAnalysis();
        if (images.size() < min || images.size() > max) {
            throw new IllegalArgumentException(
                    "images count must be between %d and %d (got %d)".formatted(min, max, images.size()));
        }

        MlAnalyzeResponse analysis = mlClient.analyze(images, video, narrative, incidentTime);

        Claim entity = mapper.toEntity(request);
        entity.setStatus(ClaimStatus.FNOL_RECEIVED);
        if (entity.getEvidences() == null) {
            entity.setEvidences(new ArrayList<>());
        }

        List<ClaimEvidence> photoEvidences = new ArrayList<>();
        for (MultipartFile file : images) {
            String name = Optional.ofNullable(file.getOriginalFilename())
                    .filter(s -> !s.isBlank())
                    .orElse("image-" + UUID.randomUUID());
            ClaimEvidence ev = new ClaimEvidence(EvidenceType.PHOTO, name);
            ev.setContentType(file.getContentType());
            ev.setSizeBytes(file.getSize());
            entity.addEvidence(ev);
            photoEvidences.add(ev);
        }
        ClaimEvidence videoEvidence = null;
        if (video != null && !video.isEmpty()) {
            String videoName = Optional.ofNullable(video.getOriginalFilename())
                    .filter(s -> !s.isBlank())
                    .orElse("video-" + UUID.randomUUID());
            videoEvidence = new ClaimEvidence(EvidenceType.VIDEO, videoName);
            videoEvidence.setContentType(video.getContentType());
            videoEvidence.setSizeBytes(video.getSize());
            entity.addEvidence(videoEvidence);
        }

        Claim saved = repository.save(entity);

        for (int i = 0; i < photoEvidences.size() && i < images.size(); i++) {
            writeEvidenceToDisk(photoEvidences.get(i), images.get(i));
        }
        if (videoEvidence != null) {
            writeEvidenceToDisk(videoEvidence, video);
        }

        persistAnalysis(saved, analysis);

        log.info("claim created (with-analysis) id={} policy={} evidences={} detections={} mlModel={}",
                saved.getId(), saved.getPolicyNumber(), saved.getEvidences().size(),
                analysis.totalDetections(), analysis.modelVersion());

        return new ClaimWithAnalysisResponse(mapper.toResponse(saved), analysis);
    }

    @Transactional
    public ClaimResponse updateStatus(UUID id, ClaimStatus newStatus) {
        Claim claim = repository.findWithEvidencesById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));

        Optional.ofNullable(newStatus)
                .filter(s -> s != claim.getStatus())
                .ifPresent(claim::setStatus);

        return mapper.toResponse(claim);
    }

    private void writeEvidenceToDisk(ClaimEvidence evidence, MultipartFile file) {
        Path target = Paths.get(claimsProperties.getUploadDir(), evidence.getId().toString());
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(target, file.getBytes());
            log.debug("wrote evidence {} ({} bytes) to {}",
                    evidence.getId(), file.getSize(), target.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to write evidence " + evidence.getId() + " to " + target.toAbsolutePath(), e);
        }
    }

    private void persistAnalysis(Claim claim, MlAnalyzeResponse response) {
        String json;
        try {
            json = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.warn("Unable to serialize MlAnalyzeResponse for persistence; storing empty payload. cause={}",
                    e.toString());
            json = "{}";
        }

        ClaimAnalysis existing = analysisRepository.findByClaimId(claim.getId()).orElse(null);
        ClaimAnalysis record = (existing != null) ? existing : new ClaimAnalysis(claim, json);
        record.setPayloadJson(json);
        record.setModelVersion(response.modelVersion());
        record.setTotalDetections(Optional.ofNullable(response.totalDetections())
                .orElseGet(() -> countDetections(response)));
        record.setProcessingTimeMs(response.processingTimeMs());

        MlAnalyzeResponse.SurveyorAssessment sa = response.surveyorAssessment();
        if (sa != null) {
            record.setSeverityVerdict(sa.severityVerdict());
            record.setRepairRecommendation(sa.repairRecommendation());
            record.setAssessmentConfidence(sa.assessmentConfidence());
        }

        MlAnalyzeResponse.CostEstimate ce = response.costEstimate();
        if (ce != null) {
            record.setCostTotalPaise(ce.totalPaise());
        }

        analysisRepository.save(record);
    }

    private static int countDetections(MlAnalyzeResponse response) {
        return Optional.ofNullable(response.images())
                .orElse(List.of())
                .stream()
                .mapToInt(img -> img.detections() == null ? 0 : img.detections().size())
                .sum();
    }

    private MlAnalyzeResponse deserializePayload(ClaimAnalysis record) {
        try {
            return objectMapper.readValue(record.getPayloadJson(), MlAnalyzeResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("Stored claim_analysis payload is not parseable for claim_id={} (id={}); returning null. cause={}",
                    record.getClaim() != null ? record.getClaim().getId() : null,
                    record.getId(), e.toString());
            return null;
        }
    }
}
