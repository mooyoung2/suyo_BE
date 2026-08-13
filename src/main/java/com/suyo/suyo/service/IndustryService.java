package com.suyo.suyo.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suyo.suyo.domain.IndustryCode;
import com.suyo.suyo.domain.IndustryCodeMapping;
import com.suyo.suyo.dto.response.IndustryGroupResponse;
import com.suyo.suyo.dto.response.IndustryItemResponse;
import com.suyo.suyo.dto.response.IndustryListResponse;
import com.suyo.suyo.repository.IndustryCodeMappingRepository;
import com.suyo.suyo.repository.IndustryCodeRepository;

import lombok.RequiredArgsConstructor;

/**
 * 서울 매출데이터와 매핑된 96개 업종만 검색 대상으로 노출한다 (2026-08-12 UX 결정).
 * 나머지 151개는 진단 결과가 반쪽만 나오는 경험을 막기 위해 애초에 노출하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndustryService {

    private final IndustryCodeRepository industryCodeRepository;
    private final IndustryCodeMappingRepository codeMappingRepository;

    public IndustryListResponse search(String query) {
        Set<String> mappedSmallCodes = codeMappingRepository.findAll().stream()
                .map(IndustryCodeMapping::getSmallCode)
                .collect(Collectors.toSet());

        List<IndustryCode> candidates = industryCodeRepository.findAllById(mappedSmallCodes).stream()
                .filter(code -> matches(code, query))
                .sorted(Comparator.comparing(IndustryCode::getLargeName).thenComparing(IndustryCode::getSmallName))
                .toList();

        LinkedHashMap<String, List<IndustryItemResponse>> byLargeCategory = candidates.stream()
                .collect(Collectors.groupingBy(
                        IndustryCode::getLargeName,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                code -> new IndustryItemResponse(code.getSmallCode(), code.getSmallName(), code.getMidName()),
                                Collectors.toList())));

        List<IndustryGroupResponse> groups = byLargeCategory.entrySet().stream()
                .map(entry -> new IndustryGroupResponse(entry.getKey(), entry.getValue()))
                .toList();

        return new IndustryListResponse(groups);
    }

    private static boolean matches(IndustryCode code, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return code.getSmallName().contains(query)
                || code.getMidName().contains(query)
                || code.getLargeName().contains(query);
    }
}
