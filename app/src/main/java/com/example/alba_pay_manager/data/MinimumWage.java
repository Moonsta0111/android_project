package com.example.alba_pay_manager.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 최저시급 정보를 저장하는 엔티티
 */
@Entity(tableName = "minimum_wages")
public class MinimumWage {
    @PrimaryKey
    private int year;
    private int wage;

    public MinimumWage(int year, int wage) {
        this.year = year;
        this.wage = wage;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getWage() {
        return wage;
    }

    public void setWage(int wage) {
        this.wage = wage;
    }

    public int getHourlyWage() {
        return wage;
    }
} 