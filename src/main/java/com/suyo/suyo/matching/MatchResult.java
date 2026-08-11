package com.suyo.suyo.matching;

import com.suyo.suyo.domain.type.MatchAccuracy;

public record MatchResult(String smallCode, String smallName, MatchAccuracy matchAccuracy, String notice) {
}
