package com.suyo.suyo.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PaymentCreateRequest(

        @NotBlank(message = "plan은 필수입니다.")
        String plan,

        @NotBlank(message = "paymentMethod는 필수입니다.")
        String paymentMethod
) {
}
