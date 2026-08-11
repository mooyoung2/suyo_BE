package com.suyo.suyo.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import com.suyo.suyo.common.exception.ErrorCode;
import com.suyo.suyo.common.exception.ErrorResponse;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 성공_응답은_success_data_error_null_형태다() throws Exception {
        ApiResponse<Map<String, Object>> response = ApiResponse.success(Map.of("analysisId", 12));

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"error\":null");
        assertThat(json).contains("\"analysisId\":12");
    }

    @Test
    void 검증_실패_응답은_에러코드와_필드메시지를_포함한다() throws Exception {
        ApiResponse<Void> response = ApiResponse.fail(
                ErrorResponse.of(ErrorCode.VALIDATION_ERROR, Map.of("itemName", "아이템명은 필수입니다.")));

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"success\":false");
        assertThat(json).contains("\"data\":null");
        assertThat(json).contains("\"code\":\"VALIDATION_ERROR\"");
        assertThat(json).contains("\"itemName\":\"아이템명은 필수입니다.\"");
    }

    @Test
    void fields_없는_에러는_fields_키가_생략된다() throws Exception {
        ApiResponse<Void> response = ApiResponse.fail(ErrorResponse.of(ErrorCode.NOT_FOUND));

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).doesNotContain("\"fields\"");
    }
}
