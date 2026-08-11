package com.suyo.suyo.matching;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.suyo.suyo.domain.IndustryCode;
import com.suyo.suyo.domain.IndustryCodeMapping;
import com.suyo.suyo.domain.type.MatchAccuracy;
import com.suyo.suyo.repository.IndustryCodeMappingRepository;
import com.suyo.suyo.repository.IndustryCodeRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * 아이템 설명(자연어) → 업종 소분류코드 매칭.
 * LLM 연동 전까지는 텍스트 포함/토큰 겹침 기반의 mock 매처로 대체한다.
 */
@Component
@RequiredArgsConstructor
public class IndustryMatcher {

    private static final String APPROXIMATE_NOTICE = "정확히 일치하는 업종 분류가 없어 유사 업종 기준으로 조회했습니다.";

    private final IndustryCodeRepository industryCodeRepository;
    private final IndustryCodeMappingRepository codeMappingRepository;

    private List<IndustryCode> industryCodes;
    private List<IndustryCodeMapping> mappings;

    @PostConstruct
    void init() {
        this.industryCodes = industryCodeRepository.findAll();
        this.mappings = codeMappingRepository.findAll();
    }

    public MatchResult match(String itemName, String problem) {
        String text = (itemName + " " + problem).toLowerCase(Locale.KOREAN);

        for (IndustryCode code : industryCodes) {
            if (text.contains(code.getSmallName().toLowerCase(Locale.KOREAN))) {
                return new MatchResult(code.getSmallCode(), code.getSmallName(), MatchAccuracy.EXACT, null);
            }
        }

        for (IndustryCodeMapping mapping : mappings) {
            if (text.contains(mapping.getSalesIndustryName().toLowerCase(Locale.KOREAN))) {
                return new MatchResult(mapping.getSmallCode(), mapping.getSmallName(), MatchAccuracy.APPROXIMATE, APPROXIMATE_NOTICE);
            }
        }

        Set<String> tokens = Arrays.stream(text.split("\\s+"))
                .filter(t -> t.length() >= 2)
                .collect(Collectors.toSet());

        IndustryCode best = industryCodes.stream()
                .max(Comparator.comparingInt(code -> overlapScore(code, tokens)))
                .filter(code -> overlapScore(code, tokens) > 0)
                .orElseGet(this::mostCommonIndustry);

        return new MatchResult(best.getSmallCode(), best.getSmallName(), MatchAccuracy.APPROXIMATE, APPROXIMATE_NOTICE);
    }

    private int overlapScore(IndustryCode code, Set<String> tokens) {
        String haystack = (code.getSmallName() + " " + code.getMidName() + " " + code.getLargeName())
                .toLowerCase(Locale.KOREAN);
        int score = 0;
        for (String token : tokens) {
            if (haystack.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private IndustryCode mostCommonIndustry() {
        return industryCodes.stream()
                .max(Comparator.comparingInt(IndustryCode::getNationalCount))
                .orElseThrow(() -> new IllegalStateException("industry_codes 데이터가 비어 있습니다."));
    }
}
