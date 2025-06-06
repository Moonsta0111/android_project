package com.example.alba_pay_manager.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 월별 급여 정보 엔티티
 */
@Entity(
    tableName = "Payroll",
    foreignKeys = @ForeignKey(
        entity = Employee.class,
        parentColumns = "id",
        childColumns = "employeeId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = {"employeeId", "year", "month"}, unique = true),
        @Index("employeeId")
    }
)
public class Payroll {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private long employeeId;
    private int year;
    private int month;
    
    private int totalMinutes;    // 총 근무시간(분)
    private int grossPay;        // 총 급여
    private int netPay;          // 실수령액
    private int insurance;       // 4대보험료
    private int incomeTax;       // 소득세
    private int localTax;        // 지방소득세

    public Payroll(long employeeId, int year, int month, 
                  int totalMinutes, int grossPay, int netPay,
                  int insurance, int incomeTax, int localTax) {
        this.employeeId = employeeId;
        this.year = year;
        this.month = month;
        this.totalMinutes = totalMinutes;
        this.grossPay = grossPay;
        this.netPay = netPay;
        this.insurance = insurance;
        this.incomeTax = incomeTax;
        this.localTax = localTax;
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

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getTotalMinutes() {
        return totalMinutes;
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