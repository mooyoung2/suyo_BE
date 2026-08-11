package com.suyo.suyo.domain;

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
@Table(name = "unverified_hypotheses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UnverifiedHypothesis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private DiagnosisResult diagnosisResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "layer", nullable = false, length = 20)
    private DiagnosisLayer layer;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "needs_verification", nullable = false)
    private Boolean needsVerification;

    @Builder
    private UnverifiedHypothesis(DiagnosisResult diagnosisResult, DiagnosisLayer layer, String description) {
        this.diagnosisResult = diagnosisResult;
        this.layer = layer;
        this.description = description;
        this.needsVerification = true;
    }

    public void markVerified() {
        this.needsVerification = false;
    }
}
