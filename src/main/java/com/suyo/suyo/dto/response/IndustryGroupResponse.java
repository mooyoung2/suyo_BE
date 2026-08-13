package com.suyo.suyo.dto.response;

import java.util.List;

public record IndustryGroupResponse(String largeCategory, List<IndustryItemResponse> industries) {
}
