package com.suyo.suyo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suyo.suyo.domain.DiagnosisResult;

public interface DiagnosisResultRepository extends JpaRepository<DiagnosisResult, Long> {

    Optional<DiagnosisResult> findByAnalysisRequestId(Long analysisRequestId);
}
