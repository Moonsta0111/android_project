package com.example.alba_pay_manager.util;

import androidx.annotation.NonNull;

import com.example.alba_pay_manager.data.AppDatabase;
import com.example.alba_pay_manager.data.Employee;
import com.example.alba_pay_manager.data.MinimumWage;
import com.example.alba_pay_manager.data.Payroll;
import com.example.alba_pay_manager.data.Shift;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 급여 계산기
 */
public class PayrollCalculator {
    private static final double INCOME_TAX_RATE = 0.033; // 3.3%
    private static final double LOCAL_TAX_RATE = 0.1;    // 지방소득세 10%
    private static final double INSURANCE_RATE = 0.045;  // 4대보험 4.5%
    private static final int NIGHT_START_HOUR = 22; // 오후 10시
    private static final int NIGHT_END_HOUR = 6;    // 오전 6시
    private static final double NIGHT_PAY_RATE = 1.5; // 야간수당 배율
    private static final double OVERTIME_PAY_RATE = 1.5; // 연장근로수당 배율
    private static final double HOLIDAY_PAY_RATE = 1.5; // 휴일근로수당 배율
    private static final double HOLIDAY_OVERTIME_PAY_RATE = 2.0; // 휴일 연장근로수당 배율
    private static final int WEEKLY_HOURS_THRESHOLD = 15; // 주휴수당 기준 시간
    private static final int DAILY_HOURS_LIMIT = 8; // 1일 근무시간 제한
    private static final int WEEKLY_HOURS_LIMIT = 40; // 1주 근무시간 제한

    private final AppDatabase database;

    public PayrollCalculator(@NonNull AppDatabase database) {
        this.database = database;
    }

    /**
     * 급여 계산 결과를 담는 클래스
     */
    public static class PayrollResult {
        private final double regularHours;      // 일반 근무시간
        private final double nightHours;        // 야간 근무시간
        private final double overtimeHours;     // 연장 근무시간
        private final double holidayHours;      // 휴일 근무시간
        private final double weeklyAllowanceHours; // 주휴수당 시간
        private final int regularPay;           // 일반 급여
        private final int nightPay;             // 야간수당
        private final int overtimePay;          // 연장근로수당
        private final int holidayPay;           // 휴일근로수당
        private final int weeklyAllowancePay;   // 주휴수당
        private final int totalPay;             // 총 급여

        public PayrollResult(double regularHours, double nightHours, double overtimeHours,
                           double holidayHours, double weeklyAllowanceHours,
                           int regularPay, int nightPay, int overtimePay,
                           int holidayPay, int weeklyAllowancePay, int totalPay) {
            this.regularHours = regularHours;
            this.nightHours = nightHours;
            this.overtimeHours = overtimeHours;
            this.holidayHours = holidayHours;
            this.weeklyAllowanceHours = weeklyAllowanceHours;
            this.regularPay = regularPay;
            this.nightPay = nightPay;
            this.overtimePay = overtimePay;
            this.holidayPay = holidayPay;
            this.weeklyAllowancePay = weeklyAllowancePay;
            this.totalPay = totalPay;
        }

        // Getters
        public double getRegularHours() { return regularHours; }
        public double getNightHours() { return nightHours; }
        public double getOvertimeHours() { return overtimeHours; }
        public double getHolidayHours() { return holidayHours; }
        public double getWeeklyAllowanceHours() { return weeklyAllowanceHours; }
        public int getRegularPay() { return regularPay; }
        public int getNightPay() { return nightPay; }
        public int getOvertimePay() { return overtimePay; }
        public int getHolidayPay() { return holidayPay; }
        public int getWeeklyAllowancePay() { return weeklyAllowancePay; }
        public int getTotalPay() { return totalPay; }
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

        return new PayrollResult(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, netPay);
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

    public static PayrollResult calculatePayroll(@NonNull Employee employee, 
                                               @NonNull List<Shift> shifts,
                                               boolean hasNightAllowance,
                                               boolean hasOvertimeAllowance,
                                               boolean hasHolidayAllowance,
                                               boolean hasWeeklyAllowance) {
        double regularHours = 0;
        double nightHours = 0;
        double overtimeHours = 0;
        double holidayHours = 0;
        Map<Integer, Double> weeklyHours = new HashMap<>(); // 주차별 근무시간

        for (Shift shift : shifts) {
            LocalDateTime start = shift.getStartTime();
            LocalDateTime end = shift.getEndTime();
            
            // 주차 계산 (ISO 주차 기준)
            int weekNumber = start.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
            double shiftHours = ChronoUnit.MINUTES.between(start, end) / 60.0;
            
            // 주차별 근무시간 누적
            weeklyHours.merge(weekNumber, shiftHours, Double::sum);

            // 휴일 여부 확인 (토요일, 일요일, 공휴일)
            boolean isHoliday = isHoliday(start.toLocalDate());
            
            // 야간 근무시간 계산 (22:00-06:00)
            if (hasNightAllowance) {
                double nightShiftHours = calculateNightHours(start, end);
                nightHours += nightShiftHours;
                shiftHours -= nightShiftHours;
            }

            // 휴일 근무시간 계산
            if (isHoliday && hasHolidayAllowance) {
                holidayHours += shiftHours;
                shiftHours = 0;
            }

            // 연장근로시간 계산
            if (hasOvertimeAllowance) {
                // 일일 연장근로
                if (shiftHours > DAILY_HOURS_LIMIT) {
                    overtimeHours += shiftHours - DAILY_HOURS_LIMIT;
                    shiftHours = DAILY_HOURS_LIMIT;
                }
            }

            regularHours += shiftHours;
        }

        // 주휴수당 계산
        double weeklyAllowanceHours = 0;
        if (hasWeeklyAllowance) {
            for (double weeklyHour : weeklyHours.values()) {
                if (weeklyHour >= WEEKLY_HOURS_THRESHOLD) {
                    if (weeklyHour >= WEEKLY_HOURS_LIMIT) {
                        weeklyAllowanceHours += 8; // 40시간 이상 근무 시 8시간
                    } else {
                        weeklyAllowanceHours += (weeklyHour / WEEKLY_HOURS_LIMIT) * 8;
                    }
                }
            }
        }

        int hourlyWage = employee.getHourlyWage();
        int regularPay = (int) (regularHours * hourlyWage);
        int nightPay = (int) (nightHours * hourlyWage * NIGHT_PAY_RATE);
        int overtimePay = (int) (overtimeHours * hourlyWage * OVERTIME_PAY_RATE);
        int holidayPay = (int) (holidayHours * hourlyWage * HOLIDAY_PAY_RATE);
        int weeklyAllowancePay = (int) (weeklyAllowanceHours * hourlyWage);

        int totalPay = regularPay + nightPay + overtimePay + holidayPay + weeklyAllowancePay;

        return new PayrollResult(regularHours, nightHours, overtimeHours, holidayHours,
                               weeklyAllowanceHours, regularPay, nightPay, overtimePay,
                               holidayPay, weeklyAllowancePay, totalPay);
    }

    private static double calculateNightHours(LocalDateTime start, LocalDateTime end) {
        double nightHours = 0;
        LocalDateTime current = start;
        
        while (current.isBefore(end)) {
            int hour = current.getHour();
            if (hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR) {
                LocalDateTime nextHour = current.plusHours(1);
                if (nextHour.isAfter(end)) {
                    nightHours += ChronoUnit.MINUTES.between(current, end) / 60.0;
                } else {
                    nightHours += 1.0;
                }
            }
            current = current.plusHours(1);
        }
        
        return nightHours;
    }

    private static boolean isHoliday(java.time.LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        // TODO: 공휴일 목록 추가 필요
    }
} 