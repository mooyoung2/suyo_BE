package com.suyo.suyo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suyo.suyo.common.ApiResponse;
import com.suyo.suyo.dto.response.CreditsResponse;
import com.suyo.suyo.service.PaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<CreditsResponse>> getCredits() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getCredits()));
    }
}
