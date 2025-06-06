package com.example.alba_pay_manager.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

/**
 * 직원 정보 엔티티
 */
@Entity(tableName = "Employee")
public class Employee {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "username")
    private String username;  // 로그인 아이디
    
    @NonNull
    @ColumnInfo(name = "password")
    private String password;  // 비밀번호
    
    @NonNull
    @ColumnInfo(name = "name")
    private String name;
    
    @ColumnInfo(name = "hourlyWage")
    private int hourlyWage;
    
    @NonNull
    @ColumnInfo(name = "role")
    private String role; // "OWNER" or "WORKER"

    @ColumnInfo(name = "phoneNumber")
    private String phoneNumber;

    @ColumnInfo(name = "bankAccount")
    private String bankAccount;

    @ColumnInfo(name = "bankName")
    private String bankName;

    @ColumnInfo(name = "bankHolder")
    private String bankHolder;

    public Employee(@NonNull String username, @NonNull String password, 
                   @NonNull String name, int hourlyWage, @NonNull String role) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.hourlyWage = hourlyWage;
        this.role = role;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    @NonNull
    public String getPassword() {
        return password;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public int getHourlyWage() {
        return hourlyWage;
    }

    public void setHourlyWage(int hourlyWage) {
        this.hourlyWage = hourlyWage;
    }

    @NonNull
    public String getRole() {
        return role;
    }

    /**
     * 비밀번호 검증
     */
    public boolean verifyPassword(@NonNull String inputPassword) {
        return password.equals(inputPassword);
    }

    public void updatePassword(@NonNull String newPassword) {
        this.password = newPassword;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public void setBankHolder(String bankHolder) {
        this.bankHolder = bankHolder;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public String getBankName() {
        return bankName;
    }

    public String getBankHolder() {
        return bankHolder;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRole(String role) {
        this.role = role;
    }
} 