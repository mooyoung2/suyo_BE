package com.suyo.suyo.scoring;

import java.math.BigDecimal;
import java.util.List;

import com.suyo.suyo.domain.type.DiagnosisLayer;

public record LayerResult(
        DiagnosisLayer layer,
        BigDecimal score,
        int maxScore,
        List<ScoredFactor> factors
) {
}
