package com.suyo.suyo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FactorResponse(
        String factor,
        String value,
        String percentile,
        Integer storeCount,
        String source,
        String referenceDate,
        String confidenceStatus
) {
}
