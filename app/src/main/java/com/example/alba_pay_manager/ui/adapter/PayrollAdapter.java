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

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PayrollAdapter extends RecyclerView.Adapter<PayrollAdapter.PayrollViewHolder> {
    private List<PayrollItem> payrollItems = new ArrayList<>();
    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;

    public PayrollAdapter() {
        dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.KOREA);
    }

    @NonNull
    @Override
    public PayrollViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payroll, parent, false);
        return new PayrollViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PayrollViewHolder holder, int position) {
        PayrollItem item = payrollItems.get(position);
        
        holder.employeeNameTextView.setText(item.employeeName);
        holder.dateRangeTextView.setText(String.format(Locale.KOREA, "%s ~ %s",
                dateFormat.format(item.startDate),
                dateFormat.format(item.endDate)));
        holder.hoursTextView.setText(String.format(Locale.KOREA, "근무 시간: %.1f시간", item.totalHours));
        holder.payTextView.setText(String.format(Locale.KOREA, "급여: %s", 
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

    static class PayrollViewHolder extends RecyclerView.ViewHolder {
        TextView employeeNameTextView;
        TextView dateRangeTextView;
        TextView hoursTextView;
        TextView payTextView;

        PayrollViewHolder(@NonNull View itemView) {
            super(itemView);
            employeeNameTextView = itemView.findViewById(R.id.employeeNameTextView);
            dateRangeTextView = itemView.findViewById(R.id.dateRangeTextView);
            hoursTextView = itemView.findViewById(R.id.hoursTextView);
            payTextView = itemView.findViewById(R.id.payTextView);
        }
    }

    public static class PayrollItem {
        public String employeeName;
        public Date startDate;
        public Date endDate;
        public double totalHours;
        public int totalPay;

        public PayrollItem(String employeeName, Date startDate, Date endDate, double totalHours, int totalPay) {
            this.employeeName = employeeName;
            this.startDate = startDate;
            this.endDate = endDate;
            this.totalHours = totalHours;
            this.totalPay = totalPay;
        }
    }
} 