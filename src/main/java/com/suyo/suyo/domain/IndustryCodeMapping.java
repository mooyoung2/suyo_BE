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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "industry_code_mapping")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndustryCodeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sales_industry_name", nullable = false, length = 100)
    private String salesIndustryName;

    @Column(name = "sales_industry_code", nullable = false, length = 20)
    private String salesIndustryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "small_code", nullable = false, insertable = false, updatable = false)
    private IndustryCode industryCode;

    @Column(name = "small_code", nullable = false, length = 10)
    private String smallCode;

    @Column(name = "small_name", nullable = false, length = 100)
    private String smallName;

    @Column(name = "mid_name", nullable = false, length = 100)
    private String midName;

    @Column(name = "national_count", nullable = false)
    private Integer nationalCount;
}
