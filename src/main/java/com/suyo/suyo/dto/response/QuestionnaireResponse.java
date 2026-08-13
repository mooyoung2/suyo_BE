package com.suyo.suyo.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionnaireResponse(
        Long questionnaireId,
        String type,
        List<QuestionnaireItemResponse> items,
        LeadingQuestionCheckResponse leadingQuestionCheck,
        LocalDateTime createdAt
) {
}
