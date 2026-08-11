package com.suyo.suyo.scoring;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.suyo.suyo.domain.IndustryCodeMapping;
import com.suyo.suyo.domain.IndustrySurvivalRate;
import com.suyo.suyo.domain.SeoulSalesQuarterly;
import com.suyo.suyo.domain.StoreCountBySgg;
import com.suyo.suyo.repository.IndustryCodeMappingRepository;
import com.suyo.suyo.repository.IndustrySurvivalRateRepository;
import com.suyo.suyo.repository.SeoulSalesQuarterlyRepository;
import com.suyo.suyo.repository.StoreCountBySggRepository;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 레이어_점수산출_설계서.md(v2 백분위 기반)의 백분위 테이블을 앱 기동 시 1회 계산해서 캐싱한다.
 * 데이터가 정적(공공데이터 1회 적재)이므로 요청마다 재계산하지 않는다.
 */
@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class ScoringCache {

    private final SeoulSalesQuarterlyRepository salesRepository;
    private final StoreCountBySggRepository storeCountRepository;
    private final IndustrySurvivalRateRepository survivalRateRepository;
    private final IndustryCodeMappingRepository codeMappingRepository;

    private Map<String, IndustryMetrics> metricsByIndustryName;
    private PercentileRanker sizeRanker;
    private PercentileRanker cagrRanker;
    private PercentileRanker momentumRanker;
    private PercentileRanker demandRanker;
    private PercentileRanker genderRanker;
    private PercentileRanker ageRanker;
    private PercentileRanker weekendRanker;
    private double weekendMedian;

    private Map<String, Map<String, Integer>> storeCountBySmallCodeAndSgg;
    private Map<String, Double> avgStoreCountBySmallCode;
    private PercentileRanker densityRanker;

    private Map<String, BigDecimal> survival5yByLargeCode;
    private PercentileRanker survivalRanker;

    private Map<String, String> salesIndustryNameBySmallCode;

    @PostConstruct
    void init() {
        buildIndustryMetrics();
        buildDensityTables();
        buildSurvivalTable();
        buildMappingTable();
        log.info("ScoringCache 초기화 완료: 업종 {}개, 소분류-자치구 조합 {}개",
                metricsByIndustryName.size(), storeCountBySmallCodeAndSgg.size());
    }

    private void buildIndustryMetrics() {
        List<SeoulSalesQuarterly> all = salesRepository.findAll();
        if (all.isEmpty()) {
            log.warn("seoul_sales_quarterly 데이터가 없어 시장/고객 레이어 백분위 테이블을 비워둡니다.");
            this.metricsByIndustryName = Map.of();
            this.sizeRanker = new PercentileRanker(List.of(), true);
            this.cagrRanker = new PercentileRanker(List.of(), true);
            this.momentumRanker = new PercentileRanker(List.of(), true);
            this.demandRanker = new PercentileRanker(List.of(), true);
            this.genderRanker = new PercentileRanker(List.of(), false);
            this.ageRanker = new PercentileRanker(List.of(), false);
            this.weekendMedian = 0;
            this.weekendRanker = new PercentileRanker(List.of(), false);
            return;
        }

        List<String> quarters = all.stream()
                .map(SeoulSalesQuarterly::getQuarter)
                .distinct()
                .sorted()
                .toList();
        List<String> l4 = quarters.subList(quarters.size() - 4, quarters.size());
        List<String> f4 = quarters.subList(0, 4);
        List<String> p4 = quarters.subList(quarters.size() - 8, quarters.size() - 4);
        double years = (quarters.size() - 4) / 4.0;
        String lastQuarter = quarters.get(quarters.size() - 1);

        Map<String, Map<String, SeoulSalesQuarterly>> byIndustry = all.stream()
                .collect(Collectors.groupingBy(SeoulSalesQuarterly::getIndustryName,
                        Collectors.toMap(SeoulSalesQuarterly::getQuarter, r -> r)));

        Map<String, IndustryMetrics> result = new HashMap<>();
        for (Map.Entry<String, Map<String, SeoulSalesQuarterly>> entry : byIndustry.entrySet()) {
            String name = entry.getKey();
            Map<String, SeoulSalesQuarterly> qs = entry.getValue();

            long recA = sumSalesAmount(qs, l4);
            long oldA = sumSalesAmount(qs, f4);
            long preA = sumSalesAmount(qs, p4);
            long recC = sumSalesCount(qs, l4);
            long oldC = sumSalesCount(qs, f4);

            double cagr = oldA > 0 ? (Math.pow((double) recA / oldA, 1.0 / years) - 1) * 100 : 0;
            double momentum = (preA > 0 && oldA > 0)
                    ? (((double) recA / preA - 1) * 100 - cagr)
                    : 0;
            double demand = oldC > 0 ? (Math.pow((double) recC / oldC, 1.0 / years) - 1) * 100 : 0;

            SeoulSalesQuarterly last = qs.get(lastQuarter);
            long male = last.getMaleAmount();
            long female = last.getFemaleAmount();
            double gender = (male + female) > 0 ? (double) Math.max(male, female) / (male + female) * 100 : 50;

            long[] ages = {last.getAge10Amount(), last.getAge20Amount(), last.getAge30Amount(),
                    last.getAge40Amount(), last.getAge50Amount(), last.getAge60Amount()};
            long ageSum = Arrays.stream(ages).sum();
            double age = ageSum > 0 ? (double) Arrays.stream(ages).max().orElse(0) / ageSum * 100 : 100;

            long weekday = last.getWeekdayAmount();
            long weekend = last.getWeekendAmount();
            double weekendRatio = (weekday + weekend) > 0 ? (double) weekend / (weekday + weekend) * 100 : 0;

            result.put(name, new IndustryMetrics(name, recA, cagr, momentum, gender, age, demand, weekendRatio));
        }
        this.metricsByIndustryName = result;

        this.sizeRanker = new PercentileRanker(values(result, IndustryMetrics::size), true);
        this.cagrRanker = new PercentileRanker(values(result, IndustryMetrics::cagr), true);
        this.momentumRanker = new PercentileRanker(values(result, IndustryMetrics::momentum), true);
        this.demandRanker = new PercentileRanker(values(result, IndustryMetrics::demand), true);
        this.genderRanker = new PercentileRanker(values(result, IndustryMetrics::gender), false);
        this.ageRanker = new PercentileRanker(values(result, IndustryMetrics::age), false);

        List<Double> weekendValues = values(result, IndustryMetrics::weekend);
        List<Double> sortedWeekend = weekendValues.stream().sorted().toList();
        this.weekendMedian = sortedWeekend.get(sortedWeekend.size() / 2);
        List<Double> weekendDistances = weekendValues.stream()
                .map(w -> Math.abs(w - weekendMedian))
                .toList();
        this.weekendRanker = new PercentileRanker(weekendDistances, false);
    }

    private void buildDensityTables() {
        List<StoreCountBySgg> all = storeCountRepository.findAll();

        Map<String, Map<String, Integer>> bySmallCode = all.stream()
                .collect(Collectors.groupingBy(StoreCountBySgg::getSmallCode,
                        Collectors.toMap(StoreCountBySgg::getSggCode, StoreCountBySgg::getStoreCount, (a, b) -> a)));
        this.storeCountBySmallCodeAndSgg = bySmallCode;

        Map<String, Double> avgBySmallCode = new HashMap<>();
        List<Double> ratios = new java.util.ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> entry : bySmallCode.entrySet()) {
            Map<String, Integer> counts = entry.getValue();
            double avg = counts.values().stream().mapToInt(Integer::intValue).average().orElse(0);
            avgBySmallCode.put(entry.getKey(), avg);
            if (avg > 0) {
                for (Integer count : counts.values()) {
                    ratios.add(count / avg);
                }
            }
        }
        this.avgStoreCountBySmallCode = avgBySmallCode;
        this.densityRanker = new PercentileRanker(ratios, false);
    }

    private void buildSurvivalTable() {
        List<IndustrySurvivalRate> all = survivalRateRepository.findAll();
        Map<String, BigDecimal> byLargeCode = all.stream()
                .filter(r -> r.getLargeCode() != null)
                .collect(Collectors.toMap(IndustrySurvivalRate::getLargeCode, IndustrySurvivalRate::getSurvival5y, (a, b) -> a));
        this.survival5yByLargeCode = byLargeCode;

        List<Double> survivalValues = all.stream()
                .filter(r -> r.getLargeCode() != null)
                .map(r -> r.getSurvival5y().doubleValue())
                .toList();
        this.survivalRanker = new PercentileRanker(survivalValues, true);
    }

    private void buildMappingTable() {
        List<IndustryCodeMapping> all = codeMappingRepository.findAll();
        this.salesIndustryNameBySmallCode = all.stream()
                .collect(Collectors.toMap(IndustryCodeMapping::getSmallCode, IndustryCodeMapping::getSalesIndustryName, (a, b) -> a));
    }

    private static long sumSalesAmount(Map<String, SeoulSalesQuarterly> qs, List<String> quarters) {
        return quarters.stream().mapToLong(q -> qs.get(q).getSalesAmount()).sum();
    }

    private static long sumSalesCount(Map<String, SeoulSalesQuarterly> qs, List<String> quarters) {
        return quarters.stream().mapToLong(q -> qs.get(q).getSalesCount()).sum();
    }

    private static List<Double> values(Map<String, IndustryMetrics> map, java.util.function.ToDoubleFunction<IndustryMetrics> extractor) {
        return map.values().stream().map(m -> extractor.applyAsDouble(m)).toList();
    }
}
