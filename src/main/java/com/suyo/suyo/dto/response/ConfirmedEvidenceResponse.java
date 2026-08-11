package com.suyo.suyo.dto.response;

public record ConfirmedEvidenceResponse(
        String layer,
        String factor,
        String value,
        String source,
        String referenceDate
) {
}
