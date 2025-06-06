package com.example.alba_pay_manager.util;

import android.util.Log;
import androidx.annotation.NonNull;

import com.example.alba_pay_manager.data.MinimumWage;
import com.example.alba_pay_manager.data.MinimumWageDao;

/**
 * 최저시급 관련 유틸리티
 */
public class WageUtils {
    private static final String TAG = "WageUtils";

    /**
     * 특정 연도의 최저시급을 조회합니다.
     * 해당 연도의 최저시급이 없으면 가장 최근 연도의 최저시급을 반환합니다.
     */
    public static int getMinimumWage(int targetYear, @NonNull MinimumWageDao dao) {
        MinimumWage mw = dao.getMinimumWage(targetYear);
        if (mw != null) {
            return mw.getHourlyWage();
        }
        MinimumWage latest = dao.getLatestMinimumWage();
        if (latest != null) {
            return latest.getHourlyWage();
        }
        // 최저시급 데이터가 없는 경우 기본값으로 2024년 최저시급 반환
        Log.w(TAG, "최저시급 데이터가 없습니다. 기본값(2024년 최저시급)을 반환합니다.");
        return 9860;
    }

    /**
     * 주어진 시급이 최저시급보다 낮은지 확인합니다.
     */
    public static boolean isBelowMinimum(int wage, int year, @NonNull MinimumWageDao dao) {
        int minWage = getMinimumWage(year, dao);
        return wage < minWage;
    }
} 