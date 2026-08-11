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

@Entity
@Table(name = "questionnaire_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionnaireItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_id", nullable = false)
    private Questionnaire questionnaire;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "purpose", columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Builder
    private QuestionnaireItem(Questionnaire questionnaire, String questionText, String purpose, Integer sortOrder) {
        this.questionnaire = questionnaire;
        this.questionText = questionText;
        this.purpose = purpose;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }
}
