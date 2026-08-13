package com.suyo.suyo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suyo.suyo.common.ApiResponse;
import com.suyo.suyo.dto.response.IndustryListResponse;
import com.suyo.suyo.service.IndustryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/industries")
@RequiredArgsConstructor
public class IndustryController {

    private final IndustryService industryService;

    @GetMapping
    public ResponseEntity<ApiResponse<IndustryListResponse>> search(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success(industryService.search(q)));
    }
}
