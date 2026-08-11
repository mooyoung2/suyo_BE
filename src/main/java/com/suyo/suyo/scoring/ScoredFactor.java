package com.suyo.suyo.scoring;

import com.suyo.suyo.domain.type.ConfidenceStatus;

public record ScoredFactor(
        String factor,
        String value,
        String percentile,
        Integer sampleSize,
        String source,
        String referenceDate,
        ConfidenceStatus confidenceStatus
) {
}
