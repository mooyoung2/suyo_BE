package com.suyo.suyo.dto.response;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long analysisId,
        boolean unlocked,
        String plan,
        int amount,
        int remainingCredits,
        LocalDateTime expiresAt
) {
}
