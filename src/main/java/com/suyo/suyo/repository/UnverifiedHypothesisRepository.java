package com.suyo.suyo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suyo.suyo.domain.UnverifiedHypothesis;

public interface UnverifiedHypothesisRepository extends JpaRepository<UnverifiedHypothesis, Long> {

    List<UnverifiedHypothesis> findByDiagnosisResultIdOrderByIdAsc(Long diagnosisResultId);
}
