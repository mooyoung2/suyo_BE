package com.suyo.suyo.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

/** 세션(익명) 단위 결제 크레딧. PACK3 결제 시 적립되고 새 분석 생성 시 자동 차감된다. */
@Entity
@Table(name = "payment_credits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentCredit {

    @Id
    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(name = "remaining_credits", nullable = false)
    private Integer remainingCredits;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private PaymentCredit(String sessionId, Integer remainingCredits, LocalDateTime expiresAt) {
        this.sessionId = sessionId;
        this.remainingCredits = remainingCredits;
        this.expiresAt = expiresAt;
    }

    public boolean hasValidCredit() {
        if (remainingCredits == null || remainingCredits <= 0) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(LocalDateTime.now());
    }

    public void addCredits(int count, LocalDateTime newExpiresAt) {
        this.remainingCredits = (this.remainingCredits == null ? 0 : this.remainingCredits) + count;
        this.expiresAt = newExpiresAt;
    }

    public void consumeOne() {
        this.remainingCredits = Math.max(0, this.remainingCredits - 1);
    }
}
