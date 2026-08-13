package com.suyo.suyo.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "questionnaire_hypotheses",
            joinColumns = @JoinColumn(name = "questionnaire_id"),
            inverseJoinColumns = @JoinColumn(name = "hypothesis_id")
    )
    private List<UnverifiedHypothesis> hypotheses = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private QuestionnaireType type;

    @Column(name = "leading_question_check_passed")
    private Boolean leadingQuestionCheckPassed;

    @Column(name = "leading_question_check_summary", columnDefinition = "TEXT")
    private String leadingQuestionCheckSummary;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Questionnaire(AnalysisRequest analysisRequest, List<UnverifiedHypothesis> hypotheses, QuestionnaireType type) {
        this.analysisRequest = analysisRequest;
        this.hypotheses = hypotheses != null ? hypotheses : new ArrayList<>();
        this.type = type;
    }

    public void applyLeadingQuestionCheck(boolean passed, String summary) {
        this.leadingQuestionCheckPassed = passed;
        this.leadingQuestionCheckSummary = summary;
    }
}
