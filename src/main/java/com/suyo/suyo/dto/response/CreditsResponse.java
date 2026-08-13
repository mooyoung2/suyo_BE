package com.suyo.suyo.dto.response;

import java.time.LocalDateTime;

public record CreditsResponse(int remainingCredits, LocalDateTime expiresAt) {
}
