package com.suyo.suyo.dto.response;

import java.util.List;

public record EvidenceResponse(
        Long analysisId,
        List<ConfirmedEvidenceResponse> confirmedEvidences,
        List<UnverifiedHypothesisResponse> unverifiedHypotheses
) {
}
