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

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ShiftAdapter extends RecyclerView.Adapter<ShiftAdapter.ShiftViewHolder> {
    private List<Shift> shifts = new ArrayList<>();
    private Map<Long, Employee> employeeMap;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;

    public ShiftAdapter() {
        dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        timeFormat = new SimpleDateFormat("a hh:mm", Locale.KOREA);
    }

    // LocalDateTime을 Date로 변환하는 헬퍼 메서드
    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    @NonNull
    @Override
    public ShiftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shift, parent, false);
        return new ShiftViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShiftViewHolder holder, int position) {
        Shift shift = shifts.get(position);
        Employee employee = employeeMap.get(shift.getEmployeeId());
        
        if (employee != null) {
            holder.employeeNameTextView.setText(employee.getName());
        }

        LocalDateTime startDateTime = shift.getStartTime();
        LocalDateTime endDateTime = shift.getEndTime();
        
        Date startDate = toDate(startDateTime);
        Date endDate = toDate(endDateTime);
        
        holder.dateTextView.setText(dateFormat.format(startDate));
        holder.timeTextView.setText(String.format(Locale.KOREA, "%s ~ %s",
                timeFormat.format(startDate),
                timeFormat.format(endDate)));

        // 근무 시간 계산 (시간과 분으로 표시)
        long diffMillis = endDate.getTime() - startDate.getTime();
        long totalMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        holder.hoursTextView.setText(String.format(Locale.KOREA, "근무 시간: %d시간 %d분", hours, minutes));
    }

    @Override
    public int getItemCount() {
        return shifts.size();
    }

    public void setShifts(List<Shift> shifts, Map<Long, Employee> employeeMap) {
        this.shifts = shifts;
        this.employeeMap = employeeMap;
        notifyDataSetChanged();
    }

    public double getTotalHours() {
        return calculateTotalHours(shifts);
    }

    private double calculateTotalHours(List<Shift> shifts) {
        long totalMinutes = 0;
        for (Shift shift : shifts) {
            if (shift.getStartTime() == null || shift.getEndTime() == null) {
                continue;
            }

            Date startDate = toDate(shift.getStartTime());
            Date endDate = toDate(shift.getEndTime());
            
            long startMillis = startDate.getTime();
            long endMillis = endDate.getTime();

            if (endMillis <= startMillis) {
                continue;
            }

            totalMinutes += TimeUnit.MILLISECONDS.toMinutes(endMillis - startMillis);
        }
        return totalMinutes / 60.0;
    }

    static class ShiftViewHolder extends RecyclerView.ViewHolder {
        TextView employeeNameTextView;
        TextView dateTextView;
        TextView timeTextView;
        TextView hoursTextView;

        ShiftViewHolder(@NonNull View itemView) {
            super(itemView);
            employeeNameTextView = itemView.findViewById(R.id.employeeNameTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            hoursTextView = itemView.findViewById(R.id.hoursTextView);
        }
    }
} 