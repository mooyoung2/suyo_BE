package com.suyo.suyo.common.exception;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, Map<String, String> fields) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, Map<String, String> fields) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), fields);
    }
}
