package com.suyo.suyo.domain;

import com.suyo.suyo.domain.type.QuestionnaireType;

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

import java.time.LocalDateTime;

@Entity
@Table(name = "questionnaires")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Questionnaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AnalysisRequest analysisRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hypothesis_id")
    private UnverifiedHypothesis hypothesis;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private QuestionnaireType type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Questionnaire(AnalysisRequest analysisRequest, UnverifiedHypothesis hypothesis, QuestionnaireType type) {
        this.analysisRequest = analysisRequest;
        this.hypothesis = hypothesis;
        this.type = type;
    }
}
