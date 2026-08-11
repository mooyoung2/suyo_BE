package com.suyo.suyo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suyo.suyo.domain.LayerEvidence;

public interface LayerEvidenceRepository extends JpaRepository<LayerEvidence, Long> {

    List<LayerEvidence> findByDiagnosisResultIdOrderByIdAsc(Long diagnosisResultId);
}
