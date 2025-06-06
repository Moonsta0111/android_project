package com.example.alba_pay_manager.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.alba_pay_manager.R;
import com.example.alba_pay_manager.data.AppDatabase;
import com.example.alba_pay_manager.data.Employee;
import com.example.alba_pay_manager.data.Shift;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class CalculatePayrollActivity extends AppCompatActivity {
    private static final String TAG = "CalculatePayrollActivity";

    private AutoCompleteTextView employeeAutoComplete;
    private TextInputEditText startDateEditText;
    private TextInputEditText endDateEditText;
    private Button calculateButton;
    private TextView totalHoursTextView;
    private TextView hourlyWageTextView;
    private TextView totalPayTextView;
    private TextView emptyView;

    private ExecutorService executorService;
    private Calendar startDate;
    private Calendar endDate;
    private Employee selectedEmployee;
    private List<Employee> workers;
    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_payroll);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("급여 계산");

        // 초기화
        executorService = Executors.newSingleThreadExecutor();
        dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.KOREA);

        initializeViews();
        setupDateAndTime();
        loadWorkers();
        setupListeners();
    }

    private void initializeViews() {
        employeeAutoComplete = findViewById(R.id.employeeAutoComplete);
        startDateEditText = findViewById(R.id.startDateEditText);
        endDateEditText = findViewById(R.id.endDateEditText);
        calculateButton = findViewById(R.id.calculateButton);
        totalHoursTextView = findViewById(R.id.totalHoursTextView);
        hourlyWageTextView = findViewById(R.id.hourlyWageTextView);
        totalPayTextView = findViewById(R.id.totalPayTextView);
        emptyView = findViewById(R.id.emptyView);
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

    private void loadWorkers() {
        executorService.execute(() -> {
            try {
                workers = AppDatabase.getInstance(this).employeeDao().getAllWorkers();
                String[] workerNames = new String[workers.size()];
                for (int i = 0; i < workers.size(); i++) {
                    workerNames[i] = workers.get(i).getName();
                }

                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            workerNames
                    );
                    employeeAutoComplete.setAdapter(adapter);
                });
            } catch (Exception e) {
                Log.e(TAG, "알바생 목록 로드 중 오류 발생", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "알바생 목록을 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupListeners() {
        // 알바생 선택
        employeeAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            selectedEmployee = workers.get(position);
            hourlyWageTextView.setText(String.format(Locale.KOREA, "시급: %s", 
                currencyFormat.format(selectedEmployee.getHourlyWage())));
        });

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

        // 급여 계산 버튼
        calculateButton.setOnClickListener(v -> calculatePayroll());
    }

    // LocalDateTime을 Date로 변환하는 헬퍼 메서드
    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    // Date를 LocalDateTime으로 변환하는 헬퍼 메서드
    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private void calculatePayroll() {
        if (selectedEmployee == null) {
            Toast.makeText(this, "알바생을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            try {
                // Calendar를 LocalDateTime으로 변환
                LocalDateTime startDateTime = startDate.getTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
                LocalDateTime endDateTime = endDate.getTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

                List<Shift> shifts = AppDatabase.getInstance(this).shiftDao()
                        .getShiftsByEmployeeAndDateRange(
                            selectedEmployee.getId(),
                            startDateTime,
                            endDateTime
                        );

                double totalHours = 0;
                for (Shift shift : shifts) {
                    Date shiftStartDate = toDate(shift.getStartTime());
                    Date shiftEndDate = toDate(shift.getEndTime());
                    long diffMillis = shiftEndDate.getTime() - shiftStartDate.getTime();
                    totalHours += diffMillis / (1000.0 * 60 * 60);
                }

                // 급여 계산
                final double finalTotalHours = totalHours;
                final int hourlyWage = selectedEmployee.getHourlyWage();
                final int totalPay = (int) (totalHours * hourlyWage);

                runOnUiThread(() -> {
                    if (shifts.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        totalHoursTextView.setVisibility(View.GONE);
                        hourlyWageTextView.setVisibility(View.GONE);
                        totalPayTextView.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        totalHoursTextView.setVisibility(View.VISIBLE);
                        hourlyWageTextView.setVisibility(View.VISIBLE);
                        totalPayTextView.setVisibility(View.VISIBLE);

                        totalHoursTextView.setText(String.format(Locale.KOREA, "총 근무 시간: %.1f시간", finalTotalHours));
                        hourlyWageTextView.setText(String.format(Locale.KOREA, "시급: %s", 
                            currencyFormat.format(hourlyWage)));
                        totalPayTextView.setText(String.format(Locale.KOREA, "총 급여: %s", 
                            currencyFormat.format(totalPay)));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "급여 계산 중 오류 발생", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "급여 계산 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        });
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
} 