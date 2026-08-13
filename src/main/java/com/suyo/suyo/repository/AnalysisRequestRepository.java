package com.suyo.suyo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.suyo.suyo.domain.AnalysisRequest;

public interface AnalysisRequestRepository extends JpaRepository<AnalysisRequest, Long> {

    Page<AnalysisRequest> findBySessionId(String sessionId, Pageable pageable);
}
