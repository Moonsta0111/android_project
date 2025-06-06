package com.example.alba_pay_manager.auth;

import android.content.Context;
import android.util.Log;

import com.example.alba_pay_manager.data.AppDatabase;
import com.example.alba_pay_manager.data.Employee;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private final Context context;

    public AuthManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public Employee authenticate(String username, String password) {
        Log.d(TAG, "Attempting to authenticate user: " + username + " with password: " + password);
        try {
            Employee employee = AppDatabase.getInstance(context).employeeDao()
                    .getEmployeeByUsername(username);
            
            if (employee != null) {
                Log.d(TAG, "Employee found: " + employee.getUsername() + ", role: " + employee.getRole());
                if (employee.getPassword().equals(password)) {
                    Log.d(TAG, "Password matches for user: " + username);
                    return employee;
                } else {
                    Log.d(TAG, "Password does not match for user: " + username);
                }
            } else {
                Log.d(TAG, "Employee not found with username: " + username);
            }
        } catch (Exception e) {
            Log.e(TAG, "Authentication error", e);
        }
        return null;
    }

    public Employee getEmployeeById(long employeeId) {
        try {
            return AppDatabase.getInstance(context).employeeDao()
                    .getEmployeeById(employeeId);
        } catch (Exception e) {
            Log.e(TAG, "Error getting employee", e);
            return null;
        }
    }
} 