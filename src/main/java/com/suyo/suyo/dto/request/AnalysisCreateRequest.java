package com.suyo.suyo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalysisCreateRequest(

        @NotBlank(message = "아이템명은 필수입니다.")
        @Size(min = 1, max = 200, message = "아이템명은 1~200자여야 합니다.")
        String itemName,

        @NotBlank(message = "해결하려는 문제는 필수입니다.")
        String problem,

        @NotBlank(message = "예상 고객은 필수입니다.")
        String targetCustomer,

        @NotBlank(message = "제공 방식은 필수입니다.")
        String deliveryMethod,

        @NotBlank(message = "지역 코드는 필수입니다.")
        String regionSggCode
) {
}
