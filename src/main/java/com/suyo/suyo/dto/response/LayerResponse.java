package com.suyo.suyo.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record LayerResponse(
        String layer,
        String layerName,
        BigDecimal score,
        int maxScore,
        String riskLevel,
        String dataScope,
        String summary,
        List<FactorResponse> factors
) {
}
