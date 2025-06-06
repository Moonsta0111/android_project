package com.example.alba_pay_manager.data;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

/**
 * 최저시급 데이터 접근 객체
 */
@Dao
public interface MinimumWageDao {
    /**
     * 특정 연도의 최저시급을 조회합니다.
     * @param year 조회할 연도
     * @return 해당 연도의 최저시급 (없으면 null)
     */
    @Nullable
    @Query("SELECT * FROM minimum_wages WHERE year = :year")
    MinimumWage getMinimumWage(int year);

    /**
     * 가장 최근 연도의 최저시급을 조회합니다.
     * @return 최신 최저시급 (없으면 null)
     */
    @Nullable
    @Query("SELECT * FROM minimum_wages ORDER BY year DESC LIMIT 1")
    MinimumWage getLatestMinimumWage();

    @Insert
    void insert(MinimumWage minimumWage);

    @Query("SELECT EXISTS(SELECT 1 FROM minimum_wages WHERE year = :year)")
    boolean hasMinimumWageForYear(int year);
} 