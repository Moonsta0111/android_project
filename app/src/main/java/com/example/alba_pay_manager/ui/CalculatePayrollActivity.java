package com.example.alba_pay_manager.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.alba_pay_manager.R;
import com.example.alba_pay_manager.data.AppDatabase;
import com.example.alba_pay_manager.data.Employee;
import com.example.alba_pay_manager.data.Shift;
import com.example.alba_pay_manager.util.AuthManager;
import com.example.alba_pay_manager.util.PayrollCalculator;
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
    private CheckBox nightAllowanceCheckBox;
    private CheckBox overtimeAllowanceCheckBox;
    private CheckBox holidayAllowanceCheckBox;
    private CheckBox weeklyAllowanceCheckBox;
    private TextView regularHoursTextView;
    private TextView nightHoursTextView;
    private TextView overtimeHoursTextView;
    private TextView holidayHoursTextView;
    private TextView weeklyAllowanceHoursTextView;
    private TextView regularPayTextView;
    private TextView nightPayTextView;
    private TextView overtimePayTextView;
    private TextView holidayPayTextView;
    private TextView weeklyAllowancePayTextView;

    private ExecutorService executorService;
    private Calendar startDate;
    private Calendar endDate;
    private Employee selectedEmployee;
    private List<Employee> workers;
    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;
    private AuthManager authManager;

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
        authManager = new AuthManager(this);

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
        nightAllowanceCheckBox = findViewById(R.id.nightAllowanceCheckBox);
        overtimeAllowanceCheckBox = findViewById(R.id.overtimeAllowanceCheckBox);
        holidayAllowanceCheckBox = findViewById(R.id.holidayAllowanceCheckBox);
        weeklyAllowanceCheckBox = findViewById(R.id.weeklyAllowanceCheckBox);
        regularHoursTextView = findViewById(R.id.regularHoursTextView);
        nightHoursTextView = findViewById(R.id.nightHoursTextView);
        overtimeHoursTextView = findViewById(R.id.overtimeHoursTextView);
        holidayHoursTextView = findViewById(R.id.holidayHoursTextView);
        weeklyAllowanceHoursTextView = findViewById(R.id.weeklyAllowanceHoursTextView);
        regularPayTextView = findViewById(R.id.regularPayTextView);
        nightPayTextView = findViewById(R.id.nightPayTextView);
        overtimePayTextView = findViewById(R.id.overtimePayTextView);
        holidayPayTextView = findViewById(R.id.holidayPayTextView);
        weeklyAllowancePayTextView = findViewById(R.id.weeklyAllowancePayTextView);

        // 파트타이머라면 알바생 선택 드롭다운 숨김
        if (!authManager.isOwner()) {
            employeeAutoComplete.setVisibility(View.GONE);
        }
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
                if (authManager.isOwner()) {
                    workers = AppDatabase.getInstance(this).employeeDao().getAllWorkers();
                } else {
                    // 파트타이머는 본인만
                    long myId = authManager.getCurrentUser().getId();
                    Employee me = AppDatabase.getInstance(this).employeeDao().getEmployeeById(myId);
                    workers = new java.util.ArrayList<>();
                    if (me != null) workers.add(me);
                }
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
                // 시작일은 00:00:00으로 설정
                Calendar startCalendar = (Calendar) startDate.clone();
                startCalendar.set(Calendar.HOUR_OF_DAY, 0);
                startCalendar.set(Calendar.MINUTE, 0);
                startCalendar.set(Calendar.SECOND, 0);
                LocalDateTime startDateTime = startCalendar.getTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

                // 종료일은 23:59:59로 설정
                Calendar endCalendar = (Calendar) endDate.clone();
                endCalendar.set(Calendar.HOUR_OF_DAY, 23);
                endCalendar.set(Calendar.MINUTE, 59);
                endCalendar.set(Calendar.SECOND, 59);
                LocalDateTime endDateTime = endCalendar.getTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

                List<Shift> shifts = AppDatabase.getInstance(this).shiftDao()
                        .getShiftsByEmployeeAndDateRange(
                            selectedEmployee.getId(),
                            startDateTime,
                            endDateTime
                        );

                // 모든 수당을 자동 적용
                PayrollCalculator.PayrollResult result = PayrollCalculator.calculatePayroll(
                    selectedEmployee,
                    shifts,
                    true, // nightAllowance
                    true, // overtimeAllowance
                    true, // holidayAllowance
                    true  // weeklyAllowance
                );

                runOnUiThread(() -> {
                    // 체크박스 UI 동기화 및 비활성화
                    nightAllowanceCheckBox.setChecked(result.getNightHours() > 0);
                    nightAllowanceCheckBox.setEnabled(false);
                    overtimeAllowanceCheckBox.setChecked(result.getOvertimeHours() > 0);
                    overtimeAllowanceCheckBox.setEnabled(false);
                    holidayAllowanceCheckBox.setChecked(result.getHolidayHours() > 0);
                    holidayAllowanceCheckBox.setEnabled(false);
                    weeklyAllowanceCheckBox.setChecked(result.getWeeklyAllowanceHours() > 0);
                    weeklyAllowanceCheckBox.setEnabled(false);

                    if (shifts.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        totalHoursTextView.setVisibility(View.GONE);
                        hourlyWageTextView.setVisibility(View.GONE);
                        totalPayTextView.setVisibility(View.GONE);
                        regularHoursTextView.setVisibility(View.GONE);
                        nightHoursTextView.setVisibility(View.GONE);
                        overtimeHoursTextView.setVisibility(View.GONE);
                        holidayHoursTextView.setVisibility(View.GONE);
                        weeklyAllowanceHoursTextView.setVisibility(View.GONE);
                        regularPayTextView.setVisibility(View.GONE);
                        nightPayTextView.setVisibility(View.GONE);
                        overtimePayTextView.setVisibility(View.GONE);
                        holidayPayTextView.setVisibility(View.GONE);
                        weeklyAllowancePayTextView.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        totalHoursTextView.setVisibility(View.VISIBLE);
                        hourlyWageTextView.setVisibility(View.VISIBLE);
                        totalPayTextView.setVisibility(View.VISIBLE);
                        regularHoursTextView.setVisibility(View.VISIBLE);
                        nightHoursTextView.setVisibility(View.VISIBLE);
                        overtimeHoursTextView.setVisibility(View.VISIBLE);
                        holidayHoursTextView.setVisibility(View.VISIBLE);
                        weeklyAllowanceHoursTextView.setVisibility(View.VISIBLE);
                        regularPayTextView.setVisibility(View.VISIBLE);
                        nightPayTextView.setVisibility(View.VISIBLE);
                        overtimePayTextView.setVisibility(View.VISIBLE);
                        holidayPayTextView.setVisibility(View.VISIBLE);
                        weeklyAllowancePayTextView.setVisibility(View.VISIBLE);

                        double totalHours = result.getRegularHours() + result.getNightHours() +
                                          result.getOvertimeHours() + result.getHolidayHours();

                        long totalMinutes = (long)(totalHours * 60);
                        long hours = totalMinutes / 60;
                        long minutes = totalMinutes % 60;
                        totalHoursTextView.setText(String.format(Locale.KOREA, "총 근무 시간: %d시간 %d분", hours, minutes));
                        hourlyWageTextView.setText(String.format(Locale.KOREA, 
                            "시급: %s", currencyFormat.format(selectedEmployee.getHourlyWage())));
                        totalPayTextView.setText(String.format(Locale.KOREA, 
                            "총 급여: %s", currencyFormat.format(result.getTotalPay())));

                        // 상세 내역 표시
                        regularHoursTextView.setText(String.format(Locale.KOREA, 
                            "일반 근무시간: %.1f시간 (%s)", 
                            result.getRegularHours(), 
                            currencyFormat.format(result.getRegularPay())));
                        
                        if (result.getNightHours() > 0) {
                            nightHoursTextView.setText(String.format(Locale.KOREA, 
                                "야간 근무시간: %.1f시간 (%s)", 
                                result.getNightHours(), 
                                currencyFormat.format(result.getNightPay())));
                            nightHoursTextView.setVisibility(View.VISIBLE);
                        } else {
                            nightHoursTextView.setVisibility(View.GONE);
                        }

                        if (result.getOvertimeHours() > 0) {
                            overtimeHoursTextView.setText(String.format(Locale.KOREA, 
                                "연장 근무시간: %.1f시간 (%s)", 
                                result.getOvertimeHours(), 
                                currencyFormat.format(result.getOvertimePay())));
                            overtimeHoursTextView.setVisibility(View.VISIBLE);
                        } else {
                            overtimeHoursTextView.setVisibility(View.GONE);
                        }

                        if (result.getHolidayHours() > 0) {
                            holidayHoursTextView.setText(String.format(Locale.KOREA, 
                                "휴일 근무시간: %.1f시간 (%s)", 
                                result.getHolidayHours(), 
                                currencyFormat.format(result.getHolidayPay())));
                            holidayHoursTextView.setVisibility(View.VISIBLE);
                        } else {
                            holidayHoursTextView.setVisibility(View.GONE);
                        }

                        // 주휴수당 표시 여부 설정
                        boolean showWeeklyAllowance = result.getWeeklyAllowanceHours() > 0;
                        weeklyAllowanceHoursTextView.setVisibility(showWeeklyAllowance ? View.VISIBLE : View.GONE);
                        weeklyAllowancePayTextView.setVisibility(showWeeklyAllowance ? View.VISIBLE : View.GONE);

                        if (showWeeklyAllowance) {
                            long weeklyMinutes = (long)(result.getWeeklyAllowanceHours() * 60);
                            long weeklyHours = weeklyMinutes / 60;
                            long weeklyMins = weeklyMinutes % 60;
                            weeklyAllowanceHoursTextView.setText(String.format(Locale.KOREA, 
                                "주휴수당 시간: %d시간 %d분", weeklyHours, weeklyMins));
                            weeklyAllowancePayTextView.setText(String.format(Locale.KOREA, 
                                "주휴수당: %s", currencyFormat.format(result.getWeeklyAllowancePay())));
                        }
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