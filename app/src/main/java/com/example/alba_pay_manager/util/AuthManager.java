package com.example.alba_pay_manager.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.alba_pay_manager.data.AppDatabase;
import com.example.alba_pay_manager.data.Employee;
import com.example.alba_pay_manager.data.EmployeeDao;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_LOGGED_IN_USER_ID = "logged_in_user_id";
    
    private final Context context;
    private final SharedPreferences prefs;
    private final EmployeeDao employeeDao;
    private Employee currentUser;

    public AuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.employeeDao = AppDatabase.getInstance(context).employeeDao();
        loadCurrentUser();
    }

    private void loadCurrentUser() {
        long userId = prefs.getLong(KEY_LOGGED_IN_USER_ID, -1);
        if (userId != -1) {
            try {
                currentUser = employeeDao.getEmployeeById(userId);
            } catch (Exception e) {
                Log.e(TAG, "사용자 정보 로드 중 오류 발생", e);
                logout();
            }
        }
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isOwner() {
        return currentUser != null && "OWNER".equals(currentUser.getRole());
    }

    public Employee getCurrentUser() {
        return currentUser;
    }

    public boolean isUserExists(String username) {
        return employeeDao.getEmployeeByUsername(username) != null;
    }

    public Employee login(String username, String password) {
        Employee employee = employeeDao.getEmployeeByUsername(username);
        if (employee == null || !employee.getPassword().equals(password)) {
            return null;
        }
        currentUser = employee;
        prefs.edit().putLong(KEY_LOGGED_IN_USER_ID, employee.getId()).apply();
        return employee;
    }

    public void logout() {
        currentUser = null;
        prefs.edit().remove(KEY_LOGGED_IN_USER_ID).apply();
    }
} 