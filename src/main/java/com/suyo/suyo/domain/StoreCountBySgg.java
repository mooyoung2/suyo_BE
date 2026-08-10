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
@Table(name = "store_counts_by_sgg")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreCountBySgg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "small_code", nullable = false, insertable = false, updatable = false)
    private IndustryCode industryCode;

    @Column(name = "small_code", nullable = false, length = 10)
    private String smallCode;

    @Column(name = "small_name", nullable = false, length = 100)
    private String smallName;

    @Column(name = "sido_code", nullable = false, length = 10)
    private String sidoCode;

    @Column(name = "sido_name", nullable = false, length = 50)
    private String sidoName;

    @Column(name = "sgg_code", nullable = false, length = 10)
    private String sggCode;

    @Column(name = "sgg_name", nullable = false, length = 50)
    private String sggName;

    @Column(name = "store_count", nullable = false)
    private Integer storeCount;
}
