package com.example.alba_pay_manager.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.time.LocalDateTime;
import java.util.List;

@Dao
public interface ShiftDao {
    @Insert
    long insert(@NonNull Shift shift);

    @Query("SELECT * FROM Shift WHERE employeeId = :employeeId " +
           "AND startTime >= :start AND endTime <= :end " +
           "ORDER BY startTime")
    @NonNull
    List<Shift> getShiftsByEmployeeAndDateRange(long employeeId, 
                                              @NonNull LocalDateTime start, 
                                              @NonNull LocalDateTime end);

    @Query("SELECT * FROM Shift WHERE startTime >= :start AND endTime <= :end " +
           "ORDER BY startTime")
    @NonNull
    List<Shift> getShiftsByDateRange(@NonNull LocalDateTime start, 
                                    @NonNull LocalDateTime end);

    @Query("SELECT * FROM Shift WHERE employeeId = :employeeId " +
           "ORDER BY startTime DESC LIMIT 1")
    @Nullable
    Shift getLatestShift(long employeeId);

    @Query("DELETE FROM Shift WHERE id = :shiftId")
    void deleteShift(long shiftId);

    @Query("SELECT * FROM Shift WHERE id = :shiftId")
    @Nullable
    Shift getShiftById(long shiftId);

    @Query("SELECT * FROM Shift WHERE employeeId = :employeeId AND startTime >= :start AND endTime <= :end")
    List<Shift> getShiftsInRange(long employeeId, LocalDateTime start, LocalDateTime end);
} 