package com.suyo.suyo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suyo.suyo.domain.AnalysisRequest;

public interface AnalysisRequestRepository extends JpaRepository<AnalysisRequest, Long> {
}
