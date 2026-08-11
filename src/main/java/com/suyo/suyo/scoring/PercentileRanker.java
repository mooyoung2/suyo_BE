package com.suyo.suyo.scoring;

import java.util.List;

/**
 * 정렬된 분포에서 값의 백분위 순위를 구한다 (bisect 기반).
 * 레이어_점수산출_설계서.md의 Python Rank 클래스를 그대로 포팅.
 */
public class PercentileRanker {

    private final double[] sorted;
    private final boolean higherIsBetter;

    public PercentileRanker(List<Double> values, boolean higherIsBetter) {
        this.sorted = values.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        this.higherIsBetter = higherIsBetter;
    }

    /** 0~100 사이의 백분위 순위. higherIsBetter=false면 방향을 뒤집는다. */
    public double percentile(double x) {
        if (sorted.length <= 1) {
            return 50.0;
        }
        int i = bisectLeft(x);
        double p = (double) i / (sorted.length - 1) * 100.0;
        return higherIsBetter ? p : 100.0 - p;
    }

    private int bisectLeft(double x) {
        int lo = 0, hi = sorted.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (sorted[mid] < x) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }
}
