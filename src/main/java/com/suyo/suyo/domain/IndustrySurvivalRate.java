package com.suyo.suyo.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "industry_survival_rates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndustrySurvivalRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "large_code", length = 10)
    private String largeCode;

    @Column(name = "large_name", nullable = false, length = 100)
    private String largeName;

    @Column(name = "stat_industry", nullable = false, length = 100)
    private String statIndustry;

    @Column(name = "survival_1y", nullable = false, precision = 4, scale = 1)
    private BigDecimal survival1y;

    @Column(name = "survival_5y", nullable = false, precision = 4, scale = 1)
    private BigDecimal survival5y;

    @Column(name = "closure_rate", nullable = false, precision = 4, scale = 1)
    private BigDecimal closureRate;

    @Column(name = "base_year", nullable = false, length = 10)
    private String baseYear;

    @Column(name = "source", nullable = false, length = 200)
    private String source;
}
