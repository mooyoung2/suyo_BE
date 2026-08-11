package com.suyo.suyo.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;

import com.suyo.suyo.common.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(ErrorResponse.of(errorCode, e.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fields.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus())
                .body(ApiResponse.fail(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, fields)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.fail(ErrorResponse.of(ErrorCode.INTERNAL_ERROR)));
    }
}
