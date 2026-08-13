package com.suyo.suyo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AnalysisListItemResponse(
        Long analysisId,
        String itemName,
        String status,
        BigDecimal totalScore,
        String verdict,
        LocalDateTime createdAt
) {
}
