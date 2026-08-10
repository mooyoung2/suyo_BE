package com.suyo.suyo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "industry_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndustryCode {

    @Id
    @Column(name = "small_code", length = 10)
    private String smallCode;

    @Column(name = "small_name", nullable = false, length = 100)
    private String smallName;

    @Column(name = "mid_code", nullable = false, length = 10)
    private String midCode;

    @Column(name = "mid_name", nullable = false, length = 100)
    private String midName;

    @Column(name = "large_code", nullable = false, length = 10)
    private String largeCode;

    @Column(name = "large_name", nullable = false, length = 100)
    private String largeName;

    @Column(name = "national_count", nullable = false)
    private Integer nationalCount;
}
