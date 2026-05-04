package com.heisenbug.claims.claim.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Mirrors the Python AnalyzeResponse in ml-services/app/schemas.py.
 * <p>
 * JSON ↔ Java naming is handled by the dedicated ML ObjectMapper in
 * {@link MlAnalyzeClient} (snake_case). This record's own field names are
 * Java-idiomatic camelCase and get serialized as camelCase for the frontend
 * by Spring's default Jackson configuration — the frontend TypeScript models
 * expect camelCase.
 * <p>
 * All numeric fields are boxed so missing/null values from the Python side
 * don't fail deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MlAnalyzeResponse(
        String modelVersion,
        List<ImageResult> images,
        Integer totalDetections,
        CostEstimate costEstimate,
        SurveyorAssessment surveyorAssessment,
        Integer processingTimeMs
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImageResult(
            String imageId,
            String source,
            Double sourceTimestampSec,
            Integer width,
            Integer height,
            List<Detection> detections,
            Integer latencyMs
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Detection(
            String label,
            Double confidence,
            String severity,
            Double severityScore
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CostEstimate(
            String currency,
            Long subtotalPaise,
            Long taxPaise,
            Long discountPaise,
            Long totalPaise,
            Long totalLowPaise,
            Long totalHighPaise,
            Double gstRate,
            String catalogVersion,
            List<String> assumptions,
            Boolean requiresHumanReview,
            List<String> reviewReasons
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SurveyorAssessment(
            String summary,
            String severityVerdict,
            String repairRecommendation,
            Double assessmentConfidence,
            String modelUsed,
            String systemPromptVersion,
            String generatedAt
    ) {}
}
