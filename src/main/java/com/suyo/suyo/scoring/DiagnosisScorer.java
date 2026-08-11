package com.suyo.suyo.scoring;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.suyo.suyo.domain.IndustryCode;
import com.suyo.suyo.domain.type.ConfidenceStatus;
import com.suyo.suyo.domain.type.DataCoverage;
import com.suyo.suyo.domain.type.DiagnosisLayer;
import com.suyo.suyo.repository.IndustryCodeRepository;

import lombok.RequiredArgsConstructor;

/**
 * 레이어_점수산출_설계서.md(v2)를 그대로 구현한 3레이어 진단 스코어러.
 * 매칭된 업종 소분류코드 + 자치구코드 하나에 대해 L1/L2(데이터)/L3 점수와 근거를 계산한다.
 */
@Component
@RequiredArgsConstructor
public class DiagnosisScorer {

    private static final int MAX_SIZE = 10;
    private static final int MAX_CAGR = 15;
    private static final int MAX_MOMENTUM = 5;
    private static final int MAX_GENDER = 5;
    private static final int MAX_AGE = 5;
    private static final int MAX_DEMAND = 8;
    private static final int MAX_WEEKEND = 10;
    private static final int MAX_DENSITY = 15;
    private static final int MAX_SURVIVAL = 15;

    private static final int L1_MAX = MAX_SIZE + MAX_CAGR + MAX_MOMENTUM;
    private static final int L2_DATA_MAX = MAX_GENDER + MAX_AGE + MAX_DEMAND + MAX_WEEKEND;
    private static final int L3_MAX = MAX_DENSITY + MAX_SURVIVAL;

    private static final int LOW_SAMPLE_THRESHOLD = 10;

    private final ScoringCache cache;
    private final IndustryCodeRepository industryCodeRepository;

    public DiagnosisComputation score(String matchedSmallCode, String sggCode, String sggName,
                                       String problem, String targetCustomer) {
        IndustryCode industryCode = industryCodeRepository.findById(matchedSmallCode)
                .orElseThrow(() -> new IllegalStateException("매칭된 업종코드가 industry_codes에 없습니다: " + matchedSmallCode));

        LayerResult competition = scoreCompetition(matchedSmallCode, sggCode, sggName, industryCode);

        String salesIndustryName = cache.getSalesIndustryNameBySmallCode().get(matchedSmallCode);
        IndustryMetrics metrics = salesIndustryName == null ? null : cache.getMetricsByIndustryName().get(salesIndustryName);

        LayerResult market;
        LayerResult customer;
        List<String> hypotheses = new ArrayList<>();

        if (metrics == null) {
            market = new LayerResult(DiagnosisLayer.MARKET, null, L1_MAX, List.of());
            customer = new LayerResult(DiagnosisLayer.CUSTOMER, null, L2_DATA_MAX, List.of());
        } else {
            market = scoreMarket(metrics);
            customer = scoreCustomer(metrics);
            hypotheses.add("\"" + problem + "\"이(가) 실제 구매로 이어지는지 아직 검증되지 않았습니다.");
            hypotheses.add("\"" + targetCustomer + "\"이(가) 비용을 지불할 의사가 있는지 아직 검증되지 않았습니다.");
        }

        boolean full = metrics != null;
        DataCoverage dataCoverage = full ? DataCoverage.FULL : DataCoverage.COMPETITION_ONLY;

        BigDecimal total = competition.score();
        if (market.score() != null) total = total.add(market.score());
        if (customer.score() != null) total = total.add(customer.score());
        total = round(total);

        String verdict = buildVerdict(full);

        return new DiagnosisComputation(total, verdict, dataCoverage, market, customer, competition, hypotheses);
    }

    private LayerResult scoreMarket(IndustryMetrics m) {
        BigDecimal sizeScore = points(cache.getSizeRanker(), m.size(), MAX_SIZE);
        BigDecimal cagrScore = points(cache.getCagrRanker(), m.cagr(), MAX_CAGR);
        BigDecimal momentumScore = points(cache.getMomentumRanker(), m.momentum(), MAX_MOMENTUM);
        BigDecimal score = round(sizeScore.add(cagrScore).add(momentumScore));

        List<ScoredFactor> factors = List.of(
                new ScoredFactor("시장 규모(최근 4분기 매출)", formatAmount(m.size()),
                        percentileText(cache.getSizeRanker(), m.size()),
                        null, "서울시 상권분석서비스", "최근 4분기", ConfidenceStatus.CONFIRMED),
                new ScoredFactor("5년 연평균 성장률(CAGR)", formatPercent(m.cagr()),
                        percentileText(cache.getCagrRanker(), m.cagr()),
                        null, "서울시 상권분석서비스", "2021~2026", ConfidenceStatus.CONFIRMED),
                new ScoredFactor("최근 모멘텀(최근1년-CAGR)", formatPercent(m.momentum()),
                        percentileText(cache.getMomentumRanker(), m.momentum()),
                        null, "서울시 상권분석서비스", "최근 1년", ConfidenceStatus.CONFIRMED)
        );
        return new LayerResult(DiagnosisLayer.MARKET, score, L1_MAX, factors);
    }

    private LayerResult scoreCustomer(IndustryMetrics m) {
        BigDecimal genderScore = points(cache.getGenderRanker(), m.gender(), MAX_GENDER);
        BigDecimal ageScore = points(cache.getAgeRanker(), m.age(), MAX_AGE);
        BigDecimal demandScore = points(cache.getDemandRanker(), m.demand(), MAX_DEMAND);
        double weekendDistance = Math.abs(m.weekend() - cache.getWeekendMedian());
        BigDecimal weekendScore = points(cache.getWeekendRanker(), weekendDistance, MAX_WEEKEND);
        BigDecimal score = round(genderScore.add(ageScore).add(demandScore).add(weekendScore));

        List<ScoredFactor> factors = List.of(
                new ScoredFactor("성별 쏠림", formatPercent(m.gender()),
                        percentileText(cache.getGenderRanker(), m.gender()),
                        null, "서울시 상권분석서비스", "최근 분기", ConfidenceStatus.CONFIRMED),
                new ScoredFactor("연령 쏠림", formatPercent(m.age()),
                        percentileText(cache.getAgeRanker(), m.age()),
                        null, "서울시 상권분석서비스", "최근 분기", ConfidenceStatus.CONFIRMED),
                new ScoredFactor("실수요 증가율(매출건수 CAGR)", formatPercent(m.demand()),
                        percentileText(cache.getDemandRanker(), m.demand()),
                        null, "서울시 상권분석서비스", "2021~2026", ConfidenceStatus.CONFIRMED),
                new ScoredFactor("소비패턴 안정성(주말비중)", formatPercent(m.weekend()),
                        percentileText(cache.getWeekendRanker(), weekendDistance),
                        null, "서울시 상권분석서비스", "최근 분기", ConfidenceStatus.CONFIRMED),
                new ScoredFactor("불편의 실체와 지불 의사", null, null, null, null, null,
                        ConfidenceStatus.INSUFFICIENT_DATA)
        );
        return new LayerResult(DiagnosisLayer.CUSTOMER, score, L2_DATA_MAX, factors);
    }

    private LayerResult scoreCompetition(String smallCode, String sggCode, String sggName, IndustryCode industryCode) {
        Map<String, Integer> countsByGu = cache.getStoreCountBySmallCodeAndSgg().getOrDefault(smallCode, Map.of());
        int n = countsByGu.getOrDefault(sggCode, 0);
        double avg = cache.getAvgStoreCountBySmallCode().getOrDefault(smallCode, 0.0);
        double ratio = avg > 0 ? n / avg : 1.0;

        BigDecimal densityScore;
        ConfidenceStatus densityConfidence;
        String densityPercentileText;
        if (n < LOW_SAMPLE_THRESHOLD) {
            densityScore = BigDecimal.valueOf(MAX_DENSITY * 0.5).setScale(1, RoundingMode.HALF_UP);
            densityConfidence = ConfidenceStatus.LOW_SAMPLE;
            densityPercentileText = "표본 적음(점포 " + n + "개)";
        } else {
            densityScore = points(cache.getDensityRanker(), ratio, MAX_DENSITY);
            densityConfidence = ConfidenceStatus.CONFIRMED;
            densityPercentileText = percentileText(cache.getDensityRanker(), ratio);
        }

        String largeCode = industryCode.getLargeCode();
        BigDecimal survival5y = cache.getSurvival5yByLargeCode().get(largeCode);
        BigDecimal survivalScore;
        ConfidenceStatus survivalConfidence;
        String survivalPercentileText = null;
        if (survival5y != null) {
            survivalScore = points(cache.getSurvivalRanker(), survival5y.doubleValue(), MAX_SURVIVAL);
            survivalConfidence = ConfidenceStatus.CONFIRMED;
            survivalPercentileText = percentileText(cache.getSurvivalRanker(), survival5y.doubleValue());
        } else {
            survivalScore = BigDecimal.ZERO;
            survivalConfidence = ConfidenceStatus.INSUFFICIENT_DATA;
        }

        BigDecimal score = round(densityScore.add(survivalScore));

        List<ScoredFactor> factors = new ArrayList<>();
        factors.add(new ScoredFactor("지역 내 동종 업소 수", n + "개 (" + sggName + ")",
                densityPercentileText, n, "소상공인시장진흥공단 상가정보", "2026년 6월", densityConfidence));
        if (survival5y != null) {
            factors.add(new ScoredFactor("업종 5년 생존율", survival5y + "%",
                    survivalPercentileText, null, "국가데이터처 기업생멸행정통계", "2023p", survivalConfidence));
        }
        return new LayerResult(DiagnosisLayer.COMPETITION, score, L3_MAX, factors);
    }

    private static BigDecimal points(PercentileRanker ranker, double value, int max) {
        double pct = ranker.percentile(value);
        return round(BigDecimal.valueOf(pct / 100.0 * max));
    }

    /** ranker.percentile()은 이미 "좋을수록 높은 값"으로 정규화되어 있으므로, 상위 %는 (100-pct)다. */
    private static String percentileText(PercentileRanker ranker, double value) {
        double pct = ranker.percentile(value);
        int upperPct = (int) Math.round(100 - pct);
        return "서울 상위 " + upperPct + "%";
    }

    private static String buildVerdict(boolean full) {
        if (!full) {
            return "시장·고객 데이터 없음";
        }
        return "고객 검증 부족";
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP);
    }

    private static String formatAmount(double amount) {
        return String.format("%,d원", Math.round(amount));
    }

    private static String formatPercent(double value) {
        return String.format("%.1f%%", value);
    }
}
