package com.example.alba_pay_manager.payroll;

import android.content.Context;
import android.util.Log;

import com.example.alba_pay_manager.data.AppDatabase;
import com.example.alba_pay_manager.data.Employee;
import com.example.alba_pay_manager.data.Shift;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public class PayrollCalculator {
    private static final String TAG = "PayrollCalculator";
    private final Context context;

    public PayrollCalculator(Context context) {
        this.context = context.getApplicationContext();
    }

    public double calculatePayroll(long employeeId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            Employee employee = AppDatabase.getInstance(context).employeeDao()
                    .getEmployeeById(employeeId);
            if (employee == null) {
                Log.e(TAG, "Employee not found: " + employeeId);
                return 0.0;
            }

            List<Shift> shifts = AppDatabase.getInstance(context).shiftDao()
                    .getShiftsByEmployeeAndDateRange(employeeId, startDate, endDate);

            double totalHours = 0;
            for (Shift shift : shifts) {
                Date shiftStart = toDate(shift.getStartTime());
                Date shiftEnd = toDate(shift.getEndTime());
                long diffMillis = shiftEnd.getTime() - shiftStart.getTime();
                totalHours += diffMillis / (1000.0 * 60 * 60);
            }

            return totalHours * employee.getHourlyWage();
        } catch (Exception e) {
            Log.e(TAG, "Error calculating payroll", e);
            return 0.0;
        }
    }

    private Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }
} 