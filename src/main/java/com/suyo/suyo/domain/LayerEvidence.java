package com.suyo.suyo.domain;

import com.suyo.suyo.domain.type.ConfidenceStatus;
import com.suyo.suyo.domain.type.DiagnosisLayer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "layer_evidences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LayerEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private DiagnosisResult diagnosisResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "layer", nullable = false, length = 20)
    private DiagnosisLayer layer;

    @Column(name = "factor", nullable = false, length = 200)
    private String factor;

    @Column(name = "factor_value", length = 200)
    private String value;

    @Column(name = "percentile", length = 50)
    private String percentile;

    @Column(name = "sample_size")
    private Integer sampleSize;

    @Column(name = "source", length = 200)
    private String source;

    @Column(name = "reference_date", length = 50)
    private String referenceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_status", length = 30)
    private ConfidenceStatus confidenceStatus;

    @Builder
    private LayerEvidence(DiagnosisResult diagnosisResult, DiagnosisLayer layer, String factor,
                           String value, String percentile, Integer sampleSize, String source,
                           String referenceDate, ConfidenceStatus confidenceStatus) {
        this.diagnosisResult = diagnosisResult;
        this.layer = layer;
        this.factor = factor;
        this.value = value;
        this.percentile = percentile;
        this.sampleSize = sampleSize;
        this.source = source;
        this.referenceDate = referenceDate;
        this.confidenceStatus = confidenceStatus;
    }
}
