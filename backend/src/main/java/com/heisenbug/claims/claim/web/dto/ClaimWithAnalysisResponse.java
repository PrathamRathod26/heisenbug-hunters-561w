package com.heisenbug.claims.claim.web.dto;

import com.heisenbug.claims.claim.client.MlAnalyzeResponse;

public record ClaimWithAnalysisResponse(
        ClaimResponse claim,
        MlAnalyzeResponse analysis
) {
}
