package com.suyo.suyo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DiagnosisResponse(
        Long analysisId,
        String itemName,
        BigDecimal totalScore,
        String verdict,
        String accessLevel,
        String aiSummary,
        String dataCoverage,
        List<LayerResponse> layers,
        LocalDateTime createdAt
) {
}
