package com.suyo.suyo.domain;

import com.suyo.suyo.domain.type.AnalysisStatus;
import com.suyo.suyo.domain.type.MatchAccuracy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "problem", columnDefinition = "TEXT")
    private String problem;

    @Column(name = "target_customer", columnDefinition = "TEXT")
    private String targetCustomer;

    @Column(name = "delivery_method", columnDefinition = "TEXT")
    private String deliveryMethod;

    @Column(name = "region_sgg_code", nullable = false, length = 10)
    private String regionSggCode;

    @Column(name = "matched_code", length = 10)
    private String matchedCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_accuracy", length = 20)
    private MatchAccuracy matchAccuracy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnalysisStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private AnalysisRequest(String itemName, String problem, String targetCustomer,
                             String deliveryMethod, String regionSggCode) {
        this.itemName = itemName;
        this.problem = problem;
        this.targetCustomer = targetCustomer;
        this.deliveryMethod = deliveryMethod;
        this.regionSggCode = regionSggCode;
        this.status = AnalysisStatus.PENDING;
    }

    public void applyIndustryMatch(String matchedCode, MatchAccuracy matchAccuracy) {
        this.matchedCode = matchedCode;
        this.matchAccuracy = matchAccuracy;
    }

    public void changeStatus(AnalysisStatus status) {
        this.status = status;
    }
}
