package com.example.alba_pay_manager.util;

import androidx.annotation.NonNull;

/**
 * 급여 계산 결과를 담는 DTO
 */
public final class PayrollResult {
    private final int grossPay;      // 총 급여
    private final int netPay;        // 실수령액
    private final int insurance;     // 4대보험료
    private final int incomeTax;     // 소득세
    private final int localTax;      // 지방소득세

    public PayrollResult(int grossPay, int netPay, int insurance, int incomeTax, int localTax) {
        this.grossPay = grossPay;
        this.netPay = netPay;
        this.insurance = insurance;
        this.incomeTax = incomeTax;
        this.localTax = localTax;
    }

    public int getGrossPay() {
        return grossPay;
    }

    public int getNetPay() {
        return netPay;
    }

    public int getInsurance() {
        return insurance;
    }

    public int getIncomeTax() {
        return incomeTax;
    }

    public int getLocalTax() {
        return localTax;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("PayrollResult{gross=%d, net=%d, insurance=%d, incomeTax=%d, localTax=%d}",
                grossPay, netPay, insurance, incomeTax, localTax);
    }
} 