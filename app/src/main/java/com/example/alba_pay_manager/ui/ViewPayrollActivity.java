package com.example.alba_pay_manager.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alba_pay_manager.R;
import com.example.alba_pay_manager.data.AppDatabase;
import com.example.alba_pay_manager.data.Employee;
import com.example.alba_pay_manager.data.Shift;
import com.example.alba_pay_manager.ui.adapter.PayrollAdapter;
import com.example.alba_pay_manager.util.AuthManager;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ViewPayrollActivity extends AppCompatActivity {
    private static final String TAG = "ViewPayrollActivity";

    private TextInputEditText startDateEditText;
    private TextInputEditText endDateEditText;
    private Button filterButton;
    private RecyclerView payrollRecyclerView;
    private TextView emptyView;
    private TextView totalPayTextView;

    private AuthManager authManager;
    private ExecutorService executorService;
    private PayrollAdapter adapter;
    private Calendar startDate;
    private Calendar endDate;
    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_payroll);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("급여 내역 조회");

        // 초기화
        executorService = Executors.newSingleThreadExecutor();
        authManager = new AuthManager(this);
        dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.KOREA);

        initializeViews();
        setupRecyclerView();
        setupDateAndTime();
        setupListeners();

        // 초기 데이터 로드
        loadPayroll();
    }

    private void initializeViews() {
        startDateEditText = findViewById(R.id.startDateEditText);
        endDateEditText = findViewById(R.id.endDateEditText);
        filterButton = findViewById(R.id.filterButton);
        payrollRecyclerView = findViewById(R.id.payrollRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        totalPayTextView = findViewById(R.id.totalPayTextView);
    }

    private void setupRecyclerView() {
        adapter = new PayrollAdapter();
        payrollRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        payrollRecyclerView.setAdapter(adapter);
    }

    private void setupDateAndTime() {
        // 현재 날짜로 초기화
        startDate = Calendar.getInstance();
        startDate.set(Calendar.DAY_OF_MONTH, 1);  // 이번 달 1일
        endDate = Calendar.getInstance();  // 오늘

        // 초기 날짜 표시
        updateDateDisplay();
    }

    private void updateDateDisplay() {
        startDateEditText.setText(dateFormat.format(startDate.getTime()));
        endDateEditText.setText(dateFormat.format(endDate.getTime()));
    }

    private void setupListeners() {
        // 시작 날짜 선택
        startDateEditText.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        startDate.set(year, month, dayOfMonth);
                        updateDateDisplay();
                    },
                    startDate.get(Calendar.YEAR),
                    startDate.get(Calendar.MONTH),
                    startDate.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });

        // 종료 날짜 선택
        endDateEditText.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        endDate.set(year, month, dayOfMonth);
                        updateDateDisplay();
                    },
                    endDate.get(Calendar.YEAR),
                    endDate.get(Calendar.MONTH),
                    endDate.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });

        // 조회 버튼
        filterButton.setOnClickListener(v -> loadPayroll());
    }

    private void loadPayroll() {
        executorService.execute(() -> {
            try {
                // Calendar를 LocalDateTime으로 변환
                LocalDateTime startDateTime = startDate.getTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
                LocalDateTime endDateTime = endDate.getTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

                // 알바생 목록 조회
                List<Employee> workers = AppDatabase.getInstance(this).employeeDao().getAllWorkers();
                Map<Long, Employee> employeeMap = new HashMap<>();
                for (Employee employee : workers) {
                    employeeMap.put(employee.getId(), employee);
                }

                // 각 알바생별 급여 계산
                List<PayrollAdapter.PayrollItem> payrollItems = new ArrayList<>();
                for (Employee employee : workers) {
                    // 근무 일정 조회
                    List<Shift> shifts = AppDatabase.getInstance(this).shiftDao()
                            .getShiftsByEmployeeAndDateRange(
                                    employee.getId(),
                                    startDateTime,
                                    endDateTime
                            );

                    if (!shifts.isEmpty()) {
                        // 총 근무 시간 계산
                        double totalHours = calculateTotalHours(shifts);
                        
                        if (totalHours > 0) {  // 근무 시간이 있는 경우만 처리
                            // 급여 계산
                            int totalPay = (int) (totalHours * employee.getHourlyWage());

                            // 급여 내역 아이템 추가
                            payrollItems.add(new PayrollAdapter.PayrollItem(
                                    employee.getName(),
                                    startDate.getTime(),
                                    endDate.getTime(),
                                    totalHours,
                                    totalPay
                            ));
                        }
                    }
                }

                runOnUiThread(() -> {
                    adapter.setPayrollItems(payrollItems);
                    updateEmptyView(payrollItems.isEmpty());
                    updateTotalPay();
                });
            } catch (Exception e) {
                Log.e(TAG, "급여 내역 조회 중 오류 발생", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "급여 내역을 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private double calculateTotalHours(List<Shift> shifts) {
        double totalHours = 0;
        for (Shift shift : shifts) {
            if (shift.getStartTime() == null || shift.getEndTime() == null) {
                continue; // 시작 시간이나 종료 시간이 null인 경우 건너뜀
            }

            Date shiftStartDate = toDate(shift.getStartTime());
            Date shiftEndDate = toDate(shift.getEndTime());
            long startMillis = shiftStartDate.getTime();
            long endMillis = shiftEndDate.getTime();

            // 종료 시간이 시작 시간보다 이전인 경우 처리
            if (endMillis <= startMillis) {
                continue; // 잘못된 시간 데이터는 건너뜀
            }

            // 밀리초를 시간으로 변환 (소수점 1자리까지)
            double hours = (endMillis - startMillis) / (1000.0 * 60 * 60);
            totalHours += Math.round(hours * 10) / 10.0; // 소수점 1자리까지 반올림
        }
        return totalHours;
    }

    private void updateEmptyView(boolean isEmpty) {
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        payrollRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateTotalPay() {
        int totalPay = adapter.getTotalPay();
        totalPayTextView.setText(String.format(Locale.KOREA, "총 급여: %s", 
            currencyFormat.format(totalPay)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // LocalDateTime을 Date로 변환하는 헬퍼 메서드
    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    // Date를 LocalDateTime으로 변환하는 헬퍼 메서드
    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
} 