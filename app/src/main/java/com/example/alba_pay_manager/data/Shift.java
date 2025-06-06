package com.example.alba_pay_manager.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;
import java.time.Duration;

/**
 * 근무 기록 엔티티
 */
@Entity(
    tableName = "Shift",
    foreignKeys = @ForeignKey(
        entity = Employee.class,
        parentColumns = "id",
        childColumns = "employeeId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index("employeeId")
)
public class Shift {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private long employeeId;
    
    @NonNull
    private LocalDateTime startTime;
    
    @NonNull
    private LocalDateTime endTime;

    public Shift(long employeeId, @NonNull LocalDateTime startTime, @NonNull LocalDateTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("시작 시간은 종료 시간보다 이전이어야 합니다.");
        }
        if (Duration.between(startTime, endTime).toHours() > 24) {
            throw new IllegalArgumentException("근무 시간은 24시간을 초과할 수 없습니다.");
        }
        this.employeeId = employeeId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getEmployeeId() {
        return employeeId;
    }

    @NonNull
    public LocalDateTime getStartTime() {
        return startTime;
    }

    @NonNull
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * 근무 시간을 분 단위로 계산
     */
    public int getDurationMinutes() {
        return (int) Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * 근무 시간이 유효한지 검증
     */
    public boolean isValid() {
        return !startTime.isAfter(endTime) && 
               Duration.between(startTime, endTime).toHours() <= 24;
    }
} 