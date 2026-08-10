package com.suyo.suyo.domain;

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
@Table(name = "seoul_sales_quarterly")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeoulSalesQuarterly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "industry_code", nullable = false, length = 20)
    private String industryCode;

    @Column(name = "industry_name", nullable = false, length = 100)
    private String industryName;

    @Column(name = "quarter", nullable = false, length = 6)
    private String quarter;

    @Column(name = "sales_amount", nullable = false)
    private Long salesAmount;

    @Column(name = "sales_count", nullable = false)
    private Long salesCount;

    @Column(name = "male_amount", nullable = false)
    private Long maleAmount;

    @Column(name = "female_amount", nullable = false)
    private Long femaleAmount;

    @Column(name = "age10_amount", nullable = false)
    private Long age10Amount;

    @Column(name = "age20_amount", nullable = false)
    private Long age20Amount;

    @Column(name = "age30_amount", nullable = false)
    private Long age30Amount;

    @Column(name = "age40_amount", nullable = false)
    private Long age40Amount;

    @Column(name = "age50_amount", nullable = false)
    private Long age50Amount;

    @Column(name = "age60_amount", nullable = false)
    private Long age60Amount;

    @Column(name = "weekday_amount", nullable = false)
    private Long weekdayAmount;

    @Column(name = "weekend_amount", nullable = false)
    private Long weekendAmount;
}
