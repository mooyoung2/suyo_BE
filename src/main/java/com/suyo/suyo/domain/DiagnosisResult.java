package com.suyo.suyo.domain;

import com.suyo.suyo.domain.type.DataCoverage;

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
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "diagnosis_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiagnosisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AnalysisRequest analysisRequest;

    @Column(name = "total_score", nullable = false, precision = 4, scale = 1)
    private BigDecimal totalScore;

    @Column(name = "market_score", precision = 4, scale = 1)
    private BigDecimal marketScore;

    @Column(name = "customer_score", precision = 4, scale = 1)
    private BigDecimal customerScore;

    @Column(name = "competition_score", nullable = false, precision = 4, scale = 1)
    private BigDecimal competitionScore;

    @Column(name = "verdict", length = 100)
    private String verdict;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_coverage", length = 50)
    private DataCoverage dataCoverage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private DiagnosisResult(AnalysisRequest analysisRequest, BigDecimal totalScore, BigDecimal marketScore,
                             BigDecimal customerScore, BigDecimal competitionScore, String verdict,
                             DataCoverage dataCoverage) {
        this.analysisRequest = analysisRequest;
        this.totalScore = totalScore;
        this.marketScore = marketScore;
        this.customerScore = customerScore;
        this.competitionScore = competitionScore;
        this.verdict = verdict;
        this.dataCoverage = dataCoverage;
    }
}
