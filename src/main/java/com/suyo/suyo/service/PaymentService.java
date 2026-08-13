package com.suyo.suyo.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suyo.suyo.common.exception.BusinessException;
import com.suyo.suyo.common.exception.ErrorCode;
import com.suyo.suyo.domain.AnalysisRequest;
import com.suyo.suyo.domain.PaymentCredit;
import com.suyo.suyo.domain.type.PaymentPlan;
import com.suyo.suyo.dto.request.PaymentCreateRequest;
import com.suyo.suyo.dto.response.CreditsResponse;
import com.suyo.suyo.dto.response.PaymentResponse;
import com.suyo.suyo.repository.AnalysisRequestRepository;
import com.suyo.suyo.repository.PaymentCreditRepository;
import com.suyo.suyo.session.SessionContext;

import lombok.RequiredArgsConstructor;

/**
 * 건별 결제(목업). 실제 PG 연동 없이 요청을 받으면 즉시 성공 처리한다 (API 명세서 10번).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private static final int SINGLE_PRICE = 9_900;
    private static final int PACK3_PRICE = 24_900;
    private static final int PACK3_BONUS_CREDITS = 2;
    private static final int CREDIT_VALID_MONTHS = 6;

    private final AnalysisRequestRepository analysisRequestRepository;
    private final PaymentCreditRepository paymentCreditRepository;
    private final SessionContext sessionContext;

    public PaymentResponse pay(Long analysisId, PaymentCreateRequest request) {
        AnalysisRequest analysis = analysisRequestRepository.findById(analysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        PaymentPlan plan = parsePlan(request.plan());
        analysis.markPaid();

        int amount;
        int remainingCredits = 0;
        LocalDateTime expiresAt = null;

        if (plan == PaymentPlan.SINGLE) {
            amount = SINGLE_PRICE;
        } else {
            amount = PACK3_PRICE;
            String sessionId = sessionContext.getSessionId();
            PaymentCredit credit = paymentCreditRepository.findBySessionId(sessionId)
                    .orElseGet(() -> paymentCreditRepository.save(
                            PaymentCredit.builder().sessionId(sessionId).remainingCredits(0).build()));
            LocalDateTime newExpiresAt = LocalDateTime.now().plusMonths(CREDIT_VALID_MONTHS);
            credit.addCredits(PACK3_BONUS_CREDITS, newExpiresAt);
            remainingCredits = credit.getRemainingCredits();
            expiresAt = credit.getExpiresAt();
        }

        return new PaymentResponse(analysis.getId(), true, plan.name(), amount, remainingCredits, expiresAt);
    }

    @Transactional(readOnly = true)
    public CreditsResponse getCredits() {
        String sessionId = sessionContext.getSessionId();
        return paymentCreditRepository.findBySessionId(sessionId)
                .filter(PaymentCredit::hasValidCredit)
                .map(c -> new CreditsResponse(c.getRemainingCredits(), c.getExpiresAt()))
                .orElse(new CreditsResponse(0, null));
    }

    private static PaymentPlan parsePlan(String plan) {
        try {
            return PaymentPlan.valueOf(plan);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "plan은 SINGLE 또는 PACK3여야 합니다.");
        }
    }
}
