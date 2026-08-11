package com.suyo.suyo.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suyo.suyo.common.SeoulDistrict;
import com.suyo.suyo.common.exception.BusinessException;
import com.suyo.suyo.common.exception.ErrorCode;
import com.suyo.suyo.domain.AnalysisRequest;
import com.suyo.suyo.domain.DiagnosisResult;
import com.suyo.suyo.domain.LayerEvidence;
import com.suyo.suyo.domain.UnverifiedHypothesis;
import com.suyo.suyo.domain.type.AnalysisStatus;
import com.suyo.suyo.domain.type.ConfidenceStatus;
import com.suyo.suyo.domain.type.DiagnosisLayer;
import com.suyo.suyo.dto.request.AnalysisCreateRequest;
import com.suyo.suyo.dto.response.AnalysisCreateResponse;
import com.suyo.suyo.dto.response.ConfirmedEvidenceResponse;
import com.suyo.suyo.dto.response.DiagnosisResponse;
import com.suyo.suyo.dto.response.EvidenceResponse;
import com.suyo.suyo.dto.response.FactorResponse;
import com.suyo.suyo.dto.response.LayerResponse;
import com.suyo.suyo.dto.response.MatchedIndustryResponse;
import com.suyo.suyo.dto.response.UnverifiedHypothesisResponse;
import com.suyo.suyo.matching.IndustryMatcher;
import com.suyo.suyo.matching.MatchResult;
import com.suyo.suyo.repository.AnalysisRequestRepository;
import com.suyo.suyo.repository.DiagnosisResultRepository;
import com.suyo.suyo.repository.LayerEvidenceRepository;
import com.suyo.suyo.repository.UnverifiedHypothesisRepository;
import com.suyo.suyo.scoring.DiagnosisComputation;
import com.suyo.suyo.scoring.DiagnosisScorer;
import com.suyo.suyo.scoring.LayerResult;
import com.suyo.suyo.scoring.RiskGrader;
import com.suyo.suyo.scoring.ScoredFactor;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AnalysisService {

    private final AnalysisRequestRepository analysisRequestRepository;
    private final DiagnosisResultRepository diagnosisResultRepository;
    private final LayerEvidenceRepository layerEvidenceRepository;
    private final UnverifiedHypothesisRepository unverifiedHypothesisRepository;
    private final IndustryMatcher industryMatcher;
    private final DiagnosisScorer diagnosisScorer;

    public AnalysisCreateResponse create(AnalysisCreateRequest request) {
        SeoulDistrict district = SeoulDistrict.findByCode(request.regionSggCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.REGION_NOT_SUPPORTED));

        AnalysisRequest analysis = AnalysisRequest.builder()
                .itemName(request.itemName())
                .problem(request.problem())
                .targetCustomer(request.targetCustomer())
                .deliveryMethod(request.deliveryMethod())
                .regionSggCode(request.regionSggCode())
                .build();
        analysis.changeStatus(AnalysisStatus.IN_PROGRESS);
        analysisRequestRepository.save(analysis);

        MatchResult match = industryMatcher.match(request.itemName(), request.problem());
        analysis.applyIndustryMatch(match.smallCode(), match.matchAccuracy());

        DiagnosisComputation computation = diagnosisScorer.score(
                match.smallCode(), request.regionSggCode(), district.getDistrictName(),
                request.problem(), request.targetCustomer());

        DiagnosisResult diagnosisResult = DiagnosisResult.builder()
                .analysisRequest(analysis)
                .totalScore(computation.totalScore())
                .marketScore(computation.market().score())
                .customerScore(computation.customer().score())
                .competitionScore(computation.competition().score())
                .verdict(computation.verdict())
                .dataCoverage(computation.dataCoverage())
                .build();
        diagnosisResultRepository.save(diagnosisResult);

        saveEvidences(diagnosisResult, computation.market());
        saveEvidences(diagnosisResult, computation.customer());
        saveEvidences(diagnosisResult, computation.competition());

        for (String description : computation.unverifiedHypotheses()) {
            unverifiedHypothesisRepository.save(UnverifiedHypothesis.builder()
                    .diagnosisResult(diagnosisResult)
                    .layer(DiagnosisLayer.CUSTOMER)
                    .description(description)
                    .build());
        }

        analysis.changeStatus(AnalysisStatus.COMPLETED);

        MatchedIndustryResponse matchedIndustryResponse = new MatchedIndustryResponse(
                match.smallCode(), match.smallName(), match.matchAccuracy().name(), match.notice());

        return new AnalysisCreateResponse(
                analysis.getId(),
                analysis.getStatus().name(),
                matchedIndustryResponse,
                "/api/analyses/" + analysis.getId() + "/diagnosis",
                analysis.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public DiagnosisResponse getDiagnosis(Long analysisId) {
        AnalysisRequest analysis = analysisRequestRepository.findById(analysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        DiagnosisResult diagnosisResult = diagnosisResultRepository.findByAnalysisRequestId(analysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_COMPLETED));
        List<LayerEvidence> evidences = layerEvidenceRepository.findByDiagnosisResultIdOrderByIdAsc(diagnosisResult.getId());

        List<LayerResponse> layers = List.of(
                toLayerResponse(DiagnosisLayer.MARKET, diagnosisResult.getMarketScore(), 30, evidences),
                toLayerResponse(DiagnosisLayer.CUSTOMER, diagnosisResult.getCustomerScore(), 40, evidences),
                toLayerResponse(DiagnosisLayer.COMPETITION, diagnosisResult.getCompetitionScore(), 30, evidences)
        );

        return new DiagnosisResponse(
                analysis.getId(),
                analysis.getItemName(),
                diagnosisResult.getTotalScore(),
                diagnosisResult.getVerdict(),
                diagnosisResult.getDataCoverage() == null ? null : diagnosisResult.getDataCoverage().name(),
                layers,
                diagnosisResult.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public EvidenceResponse getEvidence(Long analysisId) {
        analysisRequestRepository.findById(analysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        DiagnosisResult diagnosisResult = diagnosisResultRepository.findByAnalysisRequestId(analysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_COMPLETED));

        List<ConfirmedEvidenceResponse> confirmedEvidences = layerEvidenceRepository
                .findByDiagnosisResultIdOrderByIdAsc(diagnosisResult.getId()).stream()
                .filter(e -> e.getConfidenceStatus() == ConfidenceStatus.CONFIRMED)
                .map(e -> new ConfirmedEvidenceResponse(
                        e.getLayer().name(), e.getFactor(), e.getValue(), e.getSource(), e.getReferenceDate()))
                .collect(Collectors.toList());

        List<UnverifiedHypothesisResponse> unverifiedHypotheses = unverifiedHypothesisRepository
                .findByDiagnosisResultIdOrderByIdAsc(diagnosisResult.getId()).stream()
                .map(h -> new UnverifiedHypothesisResponse(
                        h.getId(), h.getLayer().name(), h.getDescription(), h.getNeedsVerification()))
                .collect(Collectors.toList());

        return new EvidenceResponse(analysisId, confirmedEvidences, unverifiedHypotheses);
    }

    private void saveEvidences(DiagnosisResult diagnosisResult, LayerResult layerResult) {
        for (ScoredFactor factor : layerResult.factors()) {
            layerEvidenceRepository.save(LayerEvidence.builder()
                    .diagnosisResult(diagnosisResult)
                    .layer(layerResult.layer())
                    .factor(factor.factor())
                    .value(factor.value())
                    .percentile(factor.percentile())
                    .sampleSize(factor.sampleSize())
                    .source(factor.source())
                    .referenceDate(factor.referenceDate())
                    .confidenceStatus(factor.confidenceStatus())
                    .build());
        }
    }

    private LayerResponse toLayerResponse(DiagnosisLayer layer, BigDecimal score, int maxScore, List<LayerEvidence> allEvidences) {
        double lowCut = switch (layer) {
            case MARKET -> RiskGrader.L1_LOW_CUT;
            case CUSTOMER -> RiskGrader.L2_LOW_CUT;
            case COMPETITION -> RiskGrader.L3_LOW_CUT;
        };
        double highCut = switch (layer) {
            case MARKET -> RiskGrader.L1_HIGH_CUT;
            case CUSTOMER -> RiskGrader.L2_HIGH_CUT;
            case COMPETITION -> RiskGrader.L3_HIGH_CUT;
        };
        var riskLevel = RiskGrader.grade(score, lowCut, highCut);

        List<FactorResponse> factors = allEvidences.stream()
                .filter(e -> e.getLayer() == layer)
                .map(e -> new FactorResponse(
                        e.getFactor(), e.getValue(), e.getPercentile(), e.getSampleSize(),
                        e.getSource(), e.getReferenceDate(),
                        e.getConfidenceStatus() == null ? null : e.getConfidenceStatus().name()))
                .collect(Collectors.toList());

        return new LayerResponse(
                layer.name(), layerName(layer), score, maxScore, riskLevel.name(),
                dataScope(layer), summary(layer, riskLevel), factors);
    }

    private static String layerName(DiagnosisLayer layer) {
        return switch (layer) {
            case MARKET -> "시장 규모·성장률";
            case CUSTOMER -> "고객(타겟)";
            case COMPETITION -> "경쟁";
        };
    }

    private static String dataScope(DiagnosisLayer layer) {
        return switch (layer) {
            case MARKET -> "서울시 전체 집계 (자치구 단위 아님)";
            case CUSTOMER -> "서울 기준 업종 벤치마크";
            case COMPETITION -> "해당 지역 실측";
        };
    }

    private static String summary(DiagnosisLayer layer, com.suyo.suyo.scoring.RiskLevel riskLevel) {
        String name = layerName(layer);
        return switch (riskLevel) {
            case LOW -> name + " 지표가 서울 평균보다 나은 편입니다.";
            case MEDIUM -> name + " 지표가 서울 평균과 비슷한 수준입니다.";
            case HIGH -> name + " 지표가 서울 평균보다 위험한 편입니다.";
            case UNKNOWN -> "이 업종은 서울 매출 데이터와 매핑되지 않아 데이터를 제공하지 않습니다.";
        };
    }
}
