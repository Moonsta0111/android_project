package com.example.alba_pay_manager.util;

import androidx.annotation.NonNull;

import com.example.alba_pay_manager.data.AppDatabase;
import com.example.alba_pay_manager.data.Employee;
import com.example.alba_pay_manager.data.MinimumWage;
import com.example.alba_pay_manager.data.Payroll;
import com.example.alba_pay_manager.data.Shift;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 급여 계산기
 */
public class PayrollCalculator {
    private static final double INCOME_TAX_RATE = 0.033; // 3.3%
    private static final double LOCAL_TAX_RATE = 0.1;    // 지방소득세 10%
    private static final double INSURANCE_RATE = 0.045;  // 4대보험 4.5%

    private final AppDatabase database;

    public PayrollCalculator(@NonNull AppDatabase database) {
        this.database = database;
    }

    /**
     * 급여 계산 결과를 담는 클래스
     */
    public static class PayrollResult {
        private final int grossPay;        // 총 급여
        private final int netPay;          // 실수령액
        private final int insurance;       // 4대보험료
        private final int incomeTax;       // 소득세
        private final int localTax;        // 지방소득세

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
    }

    /**
     * 간단한 급여 계산 (정적 메서드)
     */
    @NonNull
    public static PayrollResult calc(int minutes, int hourlyWage) {
        // 총 급여 계산
        int grossPay = (int) ((minutes / 60.0) * hourlyWage);
        if (grossPay < 0) {
            throw new IllegalArgumentException("총 급여는 음수가 될 수 없습니다.");
        }

        // 공제액 계산
        int insurance = Math.max(0, (int) (grossPay * INSURANCE_RATE));
        int incomeTax = Math.max(0, (int) (grossPay * INCOME_TAX_RATE));
        int localTax = Math.max(0, (int) (incomeTax * LOCAL_TAX_RATE));

        // 실수령액 계산
        int netPay = Math.max(0, grossPay - insurance - incomeTax - localTax);

        return new PayrollResult(grossPay, netPay, insurance, incomeTax, localTax);
    }

    /**
     * 특정 월의 급여 계산
     */
    @NonNull
    public Payroll calculateMonthlyPayroll(@NonNull Employee employee, int year, int month) {
        // 해당 월의 시작일과 종료일
        LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(1);

        // 근무 기록 조회
        List<Shift> shifts = database.shiftDao().getShiftsInRange(
                employee.getId(), startDate, endDate);

        // 총 근무시간 계산 (분)
        int totalMinutes = shifts.stream()
                .mapToInt(Shift::getDurationMinutes)
                .sum();

        // 시급 확인 (최저시급 이상)
        int hourlyWage = employee.getHourlyWage();
        MinimumWage minWage = database.minimumWageDao().getMinimumWage(year);
        if (minWage != null && hourlyWage < minWage.getHourlyWage()) {
            hourlyWage = minWage.getHourlyWage();
        }

        // 총 급여 계산
        int grossPay = (int) ((totalMinutes / 60.0) * hourlyWage);
        if (grossPay < 0) {
            throw new IllegalStateException("총 급여는 음수가 될 수 없습니다.");
        }

        // 공제액 계산
        int insurance = Math.max(0, (int) (grossPay * INSURANCE_RATE));
        int incomeTax = Math.max(0, (int) (grossPay * INCOME_TAX_RATE));
        int localTax = Math.max(0, (int) (incomeTax * LOCAL_TAX_RATE));

        // 실수령액 계산
        int netPay = Math.max(0, grossPay - insurance - incomeTax - localTax);

        return new Payroll(
                employee.getId(), year, month,
                totalMinutes, grossPay, netPay,
                insurance, incomeTax, localTax
        );
    }

    /**
     * 모든 직원의 월별 급여 계산
     */
    public void calculateAllPayrolls(int year, int month) {
        List<Employee> workers = database.employeeDao().getAllWorkers();
        for (Employee worker : workers) {
            Payroll payroll = calculateMonthlyPayroll(worker, year, month);
            database.payrollDao().upsert(payroll);
        }
    }
} 