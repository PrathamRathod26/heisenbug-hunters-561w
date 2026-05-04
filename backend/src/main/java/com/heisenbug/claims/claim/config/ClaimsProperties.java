package com.heisenbug.claims.claim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "claims")
public class ClaimsProperties {

    private int maxEvidences = 10;
    private int minEvidencesForAnalysis = 3;
    private String uploadDir = "uploads";

    public int getMaxEvidences() {
        return maxEvidences;
    }

    public void setMaxEvidences(int maxEvidences) {
        this.maxEvidences = maxEvidences;
    }

    public int getMinEvidencesForAnalysis() {
        return minEvidencesForAnalysis;
    }

    public void setMinEvidencesForAnalysis(int minEvidencesForAnalysis) {
        this.minEvidencesForAnalysis = minEvidencesForAnalysis;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }
}
