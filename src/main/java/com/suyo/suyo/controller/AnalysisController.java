package com.suyo.suyo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suyo.suyo.common.ApiResponse;
import com.suyo.suyo.dto.request.AnalysisCreateRequest;
import com.suyo.suyo.dto.response.AnalysisCreateResponse;
import com.suyo.suyo.dto.response.DiagnosisResponse;
import com.suyo.suyo.dto.response.EvidenceResponse;
import com.suyo.suyo.service.AnalysisService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping
    public ResponseEntity<ApiResponse<AnalysisCreateResponse>> create(@Valid @RequestBody AnalysisCreateRequest request) {
        AnalysisCreateResponse response = analysisService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{id}/diagnosis")
    public ResponseEntity<ApiResponse<DiagnosisResponse>> getDiagnosis(@PathVariable Long id) {
        DiagnosisResponse response = analysisService.getDiagnosis(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/evidence")
    public ResponseEntity<ApiResponse<EvidenceResponse>> getEvidence(@PathVariable Long id) {
        EvidenceResponse response = analysisService.getEvidence(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
