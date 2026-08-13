package com.suyo.suyo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suyo.suyo.common.exception.BusinessException;
import com.suyo.suyo.common.exception.ErrorCode;
import com.suyo.suyo.domain.AnalysisRequest;
import com.suyo.suyo.domain.DiagnosisResult;
import com.suyo.suyo.domain.Questionnaire;
import com.suyo.suyo.domain.QuestionnaireItem;
import com.suyo.suyo.domain.UnverifiedHypothesis;
import com.suyo.suyo.domain.type.QuestionnaireType;
import com.suyo.suyo.dto.request.QuestionnaireCreateRequest;
import com.suyo.suyo.dto.response.LeadingQuestionCheckResponse;
import com.suyo.suyo.dto.response.QuestionnaireItemResponse;
import com.suyo.suyo.dto.response.QuestionnaireResponse;
import com.suyo.suyo.llm.GeneratedItem;
import com.suyo.suyo.llm.GeneratedQuestionnaire;
import com.suyo.suyo.llm.QuestionnaireGenerationService;
import com.suyo.suyo.repository.AnalysisRequestRepository;
import com.suyo.suyo.repository.DiagnosisResultRepository;
import com.suyo.suyo.repository.QuestionnaireItemRepository;
import com.suyo.suyo.repository.QuestionnaireRepository;
import com.suyo.suyo.repository.UnverifiedHypothesisRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionnaireService {

    private final AnalysisRequestRepository analysisRequestRepository;
    private final DiagnosisResultRepository diagnosisResultRepository;
    private final UnverifiedHypothesisRepository unverifiedHypothesisRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireItemRepository questionnaireItemRepository;
    private final QuestionnaireGenerationService questionnaireGenerationService;

    public QuestionnaireResponse create(Long analysisId, QuestionnaireCreateRequest request) {
        AnalysisRequest analysis = analysisRequestRepository.findById(analysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        DiagnosisResult diagnosisResult = diagnosisResultRepository.findByAnalysisRequestId(analysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_COMPLETED));

        QuestionnaireType type = parseType(request.type());

        List<UnverifiedHypothesis> allHypotheses =
                unverifiedHypothesisRepository.findByDiagnosisResultIdOrderByIdAsc(diagnosisResult.getId());
        if (allHypotheses.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_HYPOTHESIS);
        }

        Map<Long, UnverifiedHypothesis> hypothesesById = allHypotheses.stream()
                .collect(Collectors.toMap(UnverifiedHypothesis::getId, h -> h));
        List<UnverifiedHypothesis> selected = new ArrayList<>();
        for (Long hypothesisId : request.hypothesisIds()) {
            UnverifiedHypothesis hypothesis = hypothesesById.get(hypothesisId);
            if (hypothesis == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "존재하지 않는 가설 ID입니다: " + hypothesisId);
            }
            selected.add(hypothesis);
        }

        GeneratedQuestionnaire generated = questionnaireGenerationService.generate(
                analysis.getItemName(), analysis.getProblem(), analysis.getTargetCustomer(),
                type, selected.stream().map(UnverifiedHypothesis::getDescription).toList());

        Questionnaire questionnaire = Questionnaire.builder()
                .analysisRequest(analysis)
                .hypotheses(selected)
                .type(type)
                .build();
        if (generated.leadingQuestionCheck() != null) {
            questionnaire.applyLeadingQuestionCheck(
                    generated.leadingQuestionCheck().passed(), generated.leadingQuestionCheck().summary());
        }
        questionnaireRepository.save(questionnaire);

        List<QuestionnaireItem> savedItems = new ArrayList<>();
        int order = 1;
        for (GeneratedItem item : generated.items()) {
            QuestionnaireItem entity = QuestionnaireItem.builder()
                    .questionnaire(questionnaire)
                    .questionText(item.questionText())
                    .purpose(item.purpose())
                    .sortOrder(order++)
                    .build();
            savedItems.add(questionnaireItemRepository.save(entity));
        }

        return toResponse(questionnaire, savedItems);
    }

    @Transactional(readOnly = true)
    public QuestionnaireResponse get(Long analysisId, Long questionnaireId) {
        Questionnaire questionnaire = questionnaireRepository.findByIdAndAnalysisRequestId(questionnaireId, analysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        List<QuestionnaireItem> items = questionnaireItemRepository.findByQuestionnaireIdOrderBySortOrderAsc(questionnaireId);
        return toResponse(questionnaire, items);
    }

    private static QuestionnaireType parseType(String type) {
        try {
            return QuestionnaireType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "type은 INTERVIEW 또는 SURVEY여야 합니다.");
        }
    }

    private static QuestionnaireResponse toResponse(Questionnaire questionnaire, List<QuestionnaireItem> items) {
        List<QuestionnaireItemResponse> itemResponses = items.stream()
                .map(i -> new QuestionnaireItemResponse(i.getId(), i.getSortOrder(), i.getQuestionText(), i.getPurpose()))
                .toList();
        LeadingQuestionCheckResponse checkResponse = questionnaire.getLeadingQuestionCheckPassed() == null ? null
                : new LeadingQuestionCheckResponse(
                        questionnaire.getLeadingQuestionCheckPassed(), questionnaire.getLeadingQuestionCheckSummary());
        return new QuestionnaireResponse(
                questionnaire.getId(), questionnaire.getType().name(), itemResponses, checkResponse,
                questionnaire.getCreatedAt());
    }
}
