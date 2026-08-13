package com.suyo.suyo.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record QuestionnaireCreateRequest(

        @NotEmpty(message = "hypothesisIds는 1개 이상이어야 합니다.")
        List<Long> hypothesisIds,

        @NotBlank(message = "type은 필수입니다.")
        String type
) {
}
