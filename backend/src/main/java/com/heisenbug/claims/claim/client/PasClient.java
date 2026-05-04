package com.heisenbug.claims.claim.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class PasClient {

    private static final Logger log = LoggerFactory.getLogger(PasClient.class);
    private static final String CB_NAME = "pas";

    private final RestClient restClient;

    public PasClient(@Value("${integrations.pas.base-url}") String baseUrl,
                     @Value("${integrations.pas.connect-timeout:2s}") Duration connectTimeout,
                     @Value("${integrations.pas.read-timeout:3s}") Duration readTimeout) {

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

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "verifyPolicyFallback")
    @Retry(name = CB_NAME)
    public PolicyStatusResponse verifyPolicy(String policyNumber) {
        log.debug("verifying policy {} against PAS", policyNumber);
        try {
            return restClient.get()
                    .uri("/policies/{policyNumber}/status", policyNumber)
                    .retrieve()
                    .body(PolicyStatusResponse.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return new PolicyStatusResponse(policyNumber, false, "NOT_FOUND");
            }
            throw ex;
        }
    }

    @SuppressWarnings("unused")
    private PolicyStatusResponse verifyPolicyFallback(String policyNumber, Throwable t) {
        log.warn("PAS verification unavailable for policy {} — failing closed. cause={}",
                policyNumber, t.toString());
        return new PolicyStatusResponse(policyNumber, false, "PAS_UNAVAILABLE");
    }
}
