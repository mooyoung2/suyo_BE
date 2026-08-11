package com.suyo.suyo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_score_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AnalysisRequest analysisRequest;

    @Column(name = "previous_score")
    private Integer previousScore;

    @Column(name = "updated_score")
    private Integer updatedScore;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private CustomerScoreHistory(AnalysisRequest analysisRequest, Integer previousScore,
                                  Integer updatedScore, String reason) {
        this.analysisRequest = analysisRequest;
        this.previousScore = previousScore;
        this.updatedScore = updatedScore;
        this.reason = reason;
    }
}
