package com.suyo.suyo.scoring;

import java.math.BigDecimal;
import java.util.List;

import com.suyo.suyo.domain.type.DataCoverage;

public record DiagnosisComputation(
        BigDecimal totalScore,
        String verdict,
        DataCoverage dataCoverage,
        LayerResult market,
        LayerResult customer,
        LayerResult competition,
        List<String> unverifiedHypotheses
) {
}
