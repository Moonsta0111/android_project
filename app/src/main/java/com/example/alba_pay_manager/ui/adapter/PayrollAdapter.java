package com.example.alba_pay_manager.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alba_pay_manager.R;
import com.example.alba_pay_manager.data.Employee;
import com.example.alba_pay_manager.data.Shift;
import com.example.alba_pay_manager.util.PayrollCalculator;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PayrollAdapter extends RecyclerView.Adapter<PayrollAdapter.ViewHolder> {
    private List<PayrollItem> payrollItems = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.KOREA);

    public static class PayrollItem {
        private final String employeeName;
        private final long startDate;
        private final long endDate;
        private final double totalHours;
        private final int totalPay;
        private final PayrollCalculator.PayrollResult payrollResult;

        public PayrollItem(String employeeName, long startDate, long endDate, double totalHours, int totalPay, PayrollCalculator.PayrollResult payrollResult) {
            this.employeeName = employeeName;
            this.startDate = startDate;
            this.endDate = endDate;
            this.totalHours = totalHours;
            this.totalPay = totalPay;
            this.payrollResult = payrollResult;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView employeeNameTextView;
        private final TextView dateRangeTextView;
        private final TextView totalHoursTextView;
        private final TextView totalPayTextView;
        private final TextView regularPayTextView;
        private final TextView nightPayTextView;
        private final TextView overtimePayTextView;
        private final TextView holidayPayTextView;
        private final TextView weeklyAllowancePayTextView;

        public ViewHolder(View view) {
            super(view);
            employeeNameTextView = view.findViewById(R.id.employeeNameTextView);
            dateRangeTextView = view.findViewById(R.id.dateRangeTextView);
            totalHoursTextView = view.findViewById(R.id.totalHoursTextView);
            totalPayTextView = view.findViewById(R.id.totalPayTextView);
            regularPayTextView = view.findViewById(R.id.regularPayTextView);
            nightPayTextView = view.findViewById(R.id.nightPayTextView);
            overtimePayTextView = view.findViewById(R.id.overtimePayTextView);
            holidayPayTextView = view.findViewById(R.id.holidayPayTextView);
            weeklyAllowancePayTextView = view.findViewById(R.id.weeklyAllowancePayTextView);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payroll, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PayrollItem item = payrollItems.get(position);
        PayrollCalculator.PayrollResult result = item.payrollResult;

        holder.employeeNameTextView.setText(item.employeeName);
        holder.dateRangeTextView.setText(String.format(Locale.KOREA, "%s ~ %s",
                new java.text.SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(new java.util.Date(item.startDate)),
                new java.text.SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(new java.util.Date(item.endDate))));

        // 총 근무 시간을 시간과 분으로 표시
        long totalMinutes = (long)(item.totalHours * 60);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        holder.totalHoursTextView.setText(String.format(Locale.KOREA, "총 근무 시간: %d시간 %d분", hours, minutes));

        // 기본급 표시
        holder.regularPayTextView.setText(String.format(Locale.KOREA, "기본급: %s", 
            currencyFormat.format(result.getRegularPay())));

        // 야간 수당 표시 (있는 경우에만)
        if (result.getNightHours() > 0) {
            holder.nightPayTextView.setVisibility(View.VISIBLE);
            holder.nightPayTextView.setText(String.format(Locale.KOREA, "야간 수당: %s", 
                currencyFormat.format(result.getNightPay())));
        } else {
            holder.nightPayTextView.setVisibility(View.GONE);
        }

        // 연장 수당 표시 (있는 경우에만)
        if (result.getOvertimeHours() > 0) {
            holder.overtimePayTextView.setVisibility(View.VISIBLE);
            holder.overtimePayTextView.setText(String.format(Locale.KOREA, "연장 수당: %s", 
                currencyFormat.format(result.getOvertimePay())));
        } else {
            holder.overtimePayTextView.setVisibility(View.GONE);
        }

        // 휴일 수당 표시 (있는 경우에만)
        if (result.getHolidayHours() > 0) {
            holder.holidayPayTextView.setVisibility(View.VISIBLE);
            holder.holidayPayTextView.setText(String.format(Locale.KOREA, "휴일 수당: %s", 
                currencyFormat.format(result.getHolidayPay())));
        } else {
            holder.holidayPayTextView.setVisibility(View.GONE);
        }

        // 주휴수당 표시 (있는 경우에만)
        if (result.getWeeklyAllowanceHours() > 0) {
            holder.weeklyAllowancePayTextView.setVisibility(View.VISIBLE);
            holder.weeklyAllowancePayTextView.setText(String.format(Locale.KOREA, "주휴수당: %s", 
                currencyFormat.format(result.getWeeklyAllowancePay())));
        } else {
            holder.weeklyAllowancePayTextView.setVisibility(View.GONE);
        }

        // 총 급여 표시
        holder.totalPayTextView.setText(String.format(Locale.KOREA, "총 급여: %s", 
            currencyFormat.format(item.totalPay)));
    }

    @Override
    public int getItemCount() {
        return payrollItems.size();
    }

    public void setPayrollItems(List<PayrollItem> items) {
        this.payrollItems = items;
        notifyDataSetChanged();
    }

    public int getTotalPay() {
        int total = 0;
        for (PayrollItem item : payrollItems) {
            total += item.totalPay;
        }
        return total;
    }
} 