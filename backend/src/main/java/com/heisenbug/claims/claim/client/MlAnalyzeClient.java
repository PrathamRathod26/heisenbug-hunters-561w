package com.heisenbug.claims.claim.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
public class MlAnalyzeClient {

    private static final Logger log = LoggerFactory.getLogger(MlAnalyzeClient.class);
    private static final String CB_NAME = "ml-analyze";

    /**
     * Purpose-built mapper for the ml-service wire. The Python side emits snake_case
     * (`total_detections`, `model_version`, …). Relying on the Spring Boot global
     * ObjectMapper + per-class {@code @JsonNaming} didn't take effect — something in
     * the default web MVC config overrides per-class naming on records. Using a
     * dedicated mapper here avoids fighting the global config and leaves the
     * outbound (frontend-facing) serialization in Spring's default camelCase.
     */
    private static final ObjectMapper ML_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final RestClient restClient;

    public MlAnalyzeClient(@Value("${integrations.ml-service.base-url}") String baseUrl,
                           @Value("${integrations.ml-service.connect-timeout:3s}") Duration connectTimeout,
                           @Value("${integrations.ml-service.read-timeout:60s}") Duration readTimeout) {

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout.toMillis()))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeout.toMillis()))
                .build();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "analyzeFallback")
    @Retry(name = CB_NAME)
    public MlAnalyzeResponse analyze(List<MultipartFile> images,
                                     MultipartFile video,
                                     String narrative,
                                     String incidentTime) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("at least one image is required");
        }

        MultiValueMap<String, HttpEntity<?>> body = new LinkedMultiValueMap<>();
        for (MultipartFile file : images) {
            body.add("images", filePart("images", file));
        }
        if (video != null && !video.isEmpty()) {
            body.add("video", filePart("video", video));
        }
        if (narrative != null && !narrative.isBlank()) {
            body.add("narrative", textPart("narrative", narrative));
        }
        if (incidentTime != null && !incidentTime.isBlank()) {
            body.add("incident_time", textPart("incident_time", incidentTime));
        }

        log.debug("calling ml-service /api/v1/analyze — images={} video={} narrative={} incident_time={}",
                images.size(), video != null && !video.isEmpty(),
                narrative != null && !narrative.isBlank(),
                incidentTime != null && !incidentTime.isBlank());

        byte[] raw = restClient.post()
                .uri("/api/v1/analyze")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(byte[].class);

        if (raw == null || raw.length == 0) {
            throw new IllegalStateException("ml-service returned an empty body");
        }
        try {
            MlAnalyzeResponse parsed = ML_MAPPER.readValue(raw, MlAnalyzeResponse.class);
            log.debug("ml-service response parsed — modelVersion={} totalDetections={} images={}",
                    parsed.modelVersion(), parsed.totalDetections(),
                    parsed.images() == null ? 0 : parsed.images().size());
            return parsed;
        } catch (IOException ex) {
            String preview = new String(raw, 0, Math.min(raw.length, 500), StandardCharsets.UTF_8);
            throw new IllegalStateException(
                    "Failed to parse ml-service response (first 500 bytes): " + preview, ex);
        }
    }

    @SuppressWarnings("unused")
    private MlAnalyzeResponse analyzeFallback(List<MultipartFile> images,
                                              MultipartFile video,
                                              String narrative,
                                              String incidentTime,
                                              Throwable t) {
        log.warn("ml-service unavailable ({}); returning empty analysis fallback.",
                images == null ? 0 : images.size(), t);
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root != t) {
            log.warn("  root cause: {}: {}", root.getClass().getName(), root.getMessage());
        }
        return new MlAnalyzeResponse(
                "ml-service-unavailable",
                List.of(),
                0,
                null,
                null,
                0
        );
    }

    private static HttpEntity<ByteArrayResource> filePart(String partName, MultipartFile file) {
        byte[] bytes = readBytes(file);
        String filename = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : partName + "-" + UUID.randomUUID() + ".bin";
        MediaType contentType = StringUtils.hasText(file.getContentType())
                ? MediaType.parseMediaType(file.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        final String capturedFilename = filename;
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return capturedFilename;
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("form-data")
                .name(partName)
                .filename(filename)
                .build());
        headers.setContentType(contentType);
        return new HttpEntity<>(resource, headers);
    }

    private static HttpEntity<byte[]> textPart(String partName, String value) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("form-data")
                .name(partName)
                .build());
        headers.setContentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8));
        return new HttpEntity<>(value.getBytes(StandardCharsets.UTF_8), headers);
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read uploaded file " + file.getOriginalFilename(), ex);
        }
    }
}
