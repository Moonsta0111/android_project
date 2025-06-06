package com.example.alba_pay_manager.util;

/**
 * 급여 계산에 사용되는 상수값들
 */
public final class PayrollConst {
    private PayrollConst() {} // 인스턴스화 방지

    /** 기본공제액 (월 150만원) */
    public static final int BASIC_DEDUCTION = 1_500_000;

    /** 4대보험 근로자부담률 (8.41%) */
    public static final double INSURANCE_RATE = 0.0841;

    /** 소득세율 (6%) */
    public static final double INCOME_TAX_RATE = 0.06;

    /** 세액공제액 (10.8만원) */
    public static final int INCOME_TAX_CREDIT = 108_000;

    /** 지방소득세율 (10%) */
    public static final double LOCAL_TAX_RATE = 0.10;
} 