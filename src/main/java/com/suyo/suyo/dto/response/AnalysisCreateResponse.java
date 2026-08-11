package com.suyo.suyo.dto.response;

import java.time.LocalDateTime;

public record AnalysisCreateResponse(
        Long analysisId,
        String status,
        MatchedIndustryResponse matchedIndustry,
        String diagnosisUrl,
        LocalDateTime createdAt
) {
}
