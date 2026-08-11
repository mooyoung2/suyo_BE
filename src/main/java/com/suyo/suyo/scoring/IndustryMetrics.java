package com.suyo.suyo.scoring;

/**
 * 서울매출업종(63개) 하나에 대해 계산한 원지표.
 * size: 최근 4분기 매출합, cagr: 5년 연평균 성장률(%), momentum: 최근 모멘텀(최근1년-CAGR, %p)
 * gender/age: 성별·연령 쏠림(%, 최댓값 비중), demand: 매출건수 CAGR(%), weekend: 주말매출 비중(%)
 */
public record IndustryMetrics(
        String industryName,
        double size,
        double cagr,
        double momentum,
        double gender,
        double age,
        double demand,
        double weekend
) {
}
