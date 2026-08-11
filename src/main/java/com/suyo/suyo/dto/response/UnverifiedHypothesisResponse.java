package com.suyo.suyo.dto.response;

public record UnverifiedHypothesisResponse(
        Long hypothesisId,
        String layer,
        String description,
        boolean needsVerification
) {
}
