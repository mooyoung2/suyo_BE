package com.suyo.suyo.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "필수 입력값이 누락되었거나 형식이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "해당 분석·질문지를 찾을 수 없습니다."),
    ANALYSIS_NOT_COMPLETED(HttpStatus.CONFLICT, "진단이 아직 끝나지 않았습니다."),
    NO_HYPOTHESIS(HttpStatus.CONFLICT, "생성할 미검증 가설이 없습니다."),
    LLM_ERROR(HttpStatus.BAD_GATEWAY, "LLM 호출에 실패했습니다."),
    REGION_NOT_SUPPORTED(HttpStatus.UNPROCESSABLE_CONTENT, "서울 25개 자치구 외 지역은 지원하지 않습니다."),
    INDUSTRY_NOT_SUPPORTED(HttpStatus.UNPROCESSABLE_CONTENT, "매핑된 96개 업종 밖의 업종 코드입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
