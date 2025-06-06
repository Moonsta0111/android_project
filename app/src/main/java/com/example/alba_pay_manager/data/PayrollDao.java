package com.example.alba_pay_manager.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PayrollDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(@NonNull Payroll payroll);

    @Query("SELECT * FROM Payroll WHERE employeeId = :employeeId " +
           "AND year = :year AND month = :month")
    @Nullable
    Payroll getPayroll(long employeeId, int year, int month);

    @Query("SELECT * FROM Payroll WHERE employeeId = :employeeId " +
           "ORDER BY year DESC, month DESC LIMIT 1")
    @Nullable
    Payroll getLatestPayroll(long employeeId);

    @Query("SELECT * FROM Payroll WHERE year = :year AND month = :month " +
           "ORDER BY employeeId")
    @NonNull
    List<Payroll> getPayrollsByMonth(int year, int month);

    @Query("SELECT * FROM Payroll WHERE employeeId = :employeeId " +
           "ORDER BY year DESC, month DESC")
    @NonNull
    List<Payroll> getPayrollHistory(long employeeId);
} 