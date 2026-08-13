package com.suyo.suyo.dto.response;

import java.util.List;

public record AnalysisListResponse(
        List<AnalysisListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
