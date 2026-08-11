package com.suyo.suyo.scoring;

import java.math.BigDecimal;

/**
 * 등급(LOW/MEDIUM/HIGH)은 저장하지 않고 조회 시 계산한다 (레이어_점수산출_설계서.md 8-5).
 * 경계값은 2,395개 조합 실측 분포의 P33/P67 (설계서 2-2 확정값).
 */
public final class RiskGrader {

    public static final double L1_LOW_CUT = 15.6;
    public static final double L1_HIGH_CUT = 21.6;
    public static final double L2_LOW_CUT = 13.4;
    public static final double L2_HIGH_CUT = 17.6;
    public static final double L3_LOW_CUT = 9.2;
    public static final double L3_HIGH_CUT = 14.5;
    public static final double TOTAL_LOW_CUT = 41.9;
    public static final double TOTAL_HIGH_CUT = 50.3;

    private RiskGrader() {
    }

    public static RiskLevel grade(BigDecimal score, double lowCut, double highCut) {
        if (score == null) {
            return RiskLevel.UNKNOWN;
        }
        double value = score.doubleValue();
        if (value >= highCut) return RiskLevel.LOW;
        if (value >= lowCut) return RiskLevel.MEDIUM;
        return RiskLevel.HIGH;
    }
}
