package com.example.alba_pay_manager.util;

import android.util.Log;
import androidx.annotation.NonNull;

import com.example.alba_pay_manager.data.Employee;
import com.example.alba_pay_manager.data.Shift;

import java.time.LocalDateTime;

/**
 * 데이터 검증 유틸리티
 */
public class ValidationUtils {
    private static final String TAG = "ValidationUtils";
    
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_SHIFT_HOURS = 24;
    private static final int MIN_HOURLY_WAGE = 5000;

    /**
     * 직원 정보 유효성 검사
     */
    public static boolean isValidEmployee(@NonNull Employee employee) {
        try {
            // 아이디 검증
            if (!isValidUsername(employee.getUsername())) {
                Log.w(TAG, "유효하지 않은 아이디: " + employee.getUsername());
                return false;
            }

            // 이름 검증
            if (!isValidName(employee.getName())) {
                Log.w(TAG, "유효하지 않은 이름: " + employee.getName());
                return false;
            }

            // 시급 검증
            if (!isValidHourlyWage(employee.getHourlyWage())) {
                Log.w(TAG, "유효하지 않은 시급: " + employee.getHourlyWage());
                return false;
            }

            // 역할 검증
            if (!isValidRole(employee.getRole())) {
                Log.w(TAG, "유효하지 않은 역할: " + employee.getRole());
                return false;
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "직원 정보 검증 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 아이디 유효성 검사
     */
    public static boolean isValidUsername(@NonNull String username) {
        // 아이디는 3~20자 영문, 숫자만 허용
        return username.matches("^[a-zA-Z0-9]{3,20}$");
    }

    /**
     * 이름 유효성 검사
     */
    public static boolean isValidName(@NonNull String name) {
        // 이름은 2~20자 한글, 영문만 허용
        return name.matches("^[가-힣a-zA-Z]{2,20}$");
    }

    /**
     * 시급 유효성 검사
     */
    public static boolean isValidHourlyWage(int hourlyWage) {
        // 시급은 0 이상이어야 함
        return hourlyWage >= 0;
    }

    /**
     * 역할 유효성 검사
     */
    public static boolean isValidRole(@NonNull String role) {
        // 역할은 "OWNER" 또는 "WORKER"만 허용
        return "OWNER".equals(role) || "WORKER".equals(role);
    }

    /**
     * 비밀번호 유효성 검사
     */
    public static boolean isValidPassword(@NonNull String password) {
        // 비밀번호는 6자 이상이어야 함
        return password.length() >= MIN_PASSWORD_LENGTH;
    }

    /**
     * 근무 기록 검증
     */
    @NonNull
    public static String validateShift(@NonNull Shift shift) {
        LocalDateTime start = shift.getStartTime();
        LocalDateTime end = shift.getEndTime();

        if (start.isAfter(end)) {
            return "근무 시작 시간이 종료 시간보다 늦을 수 없습니다.";
        }

        int durationHours = shift.getDurationMinutes() / 60;
        if (durationHours > MAX_SHIFT_HOURS) {
            return "근무 시간은 " + MAX_SHIFT_HOURS + "시간을 초과할 수 없습니다.";
        }

        if (start.isBefore(LocalDateTime.now().minusYears(1))) {
            return "1년 이상 지난 날짜의 근무 기록은 등록할 수 없습니다.";
        }

        if (end.isAfter(LocalDateTime.now().plusDays(1))) {
            return "미래의 근무 기록은 등록할 수 없습니다.";
        }

        return "";
    }

    /**
     * 시급 검증
     */
    @NonNull
    public static String validateHourlyWage(int hourlyWage, int minimumWage) {
        if (hourlyWage < minimumWage) {
            return "시급은 최저시급(" + minimumWage + "원) 이상이어야 합니다.";
        }
        if (hourlyWage > minimumWage * 10) {
            return "시급이 비정상적으로 높습니다.";
        }
        return "";
    }
} 