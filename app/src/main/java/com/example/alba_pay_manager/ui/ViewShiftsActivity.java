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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alba_pay_manager.R;
import com.example.alba_pay_manager.data.AppDatabase;
import com.example.alba_pay_manager.data.Employee;
import com.example.alba_pay_manager.data.Shift;
import com.example.alba_pay_manager.ui.adapter.ShiftAdapter;
import com.example.alba_pay_manager.util.AuthManager;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewShiftsActivity extends AppCompatActivity {
    private static final String TAG = "ViewShiftsActivity";

    private AutoCompleteTextView employeeAutoComplete;
    private TextInputEditText startDateEditText;
    private TextInputEditText endDateEditText;
    private Button filterButton;
    private RecyclerView shiftsRecyclerView;
    private TextView emptyView;
    private TextView totalHoursTextView;

    private AuthManager authManager;
    private ExecutorService executorService;
    private ShiftAdapter adapter;
    private Calendar startDate;
    private Calendar endDate;
    private Employee selectedEmployee;
    private List<Employee> workers;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_shifts);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("근무 일정 조회");

        // 초기화
        executorService = Executors.newSingleThreadExecutor();
        authManager = new AuthManager(this);
        dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);

        initializeViews();
        setupRecyclerView();
        setupDateAndTime();
        loadWorkers();
        setupListeners();

        // 초기 데이터 로드
        loadShifts();
    }

    private void initializeViews() {
        employeeAutoComplete = findViewById(R.id.employeeAutoComplete);
        startDateEditText = findViewById(R.id.startDateEditText);
        endDateEditText = findViewById(R.id.endDateEditText);
        filterButton = findViewById(R.id.filterButton);
        shiftsRecyclerView = findViewById(R.id.shiftsRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        totalHoursTextView = findViewById(R.id.totalHoursTextView);
    }

    private void setupRecyclerView() {
        adapter = new ShiftAdapter();
        shiftsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        shiftsRecyclerView.setAdapter(adapter);
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

        // 조회 버튼
        filterButton.setOnClickListener(v -> loadShifts());
    }

    private void loadShifts() {
        executorService.execute(() -> {
            try {
                // Calendar를 LocalDateTime으로 변환
                LocalDateTime startDateTime = startDate.getTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
                LocalDateTime endDateTime = endDate.getTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

                List<Shift> shifts;
                if (selectedEmployee != null) {
                    shifts = AppDatabase.getInstance(this).shiftDao()
                            .getShiftsByEmployeeAndDateRange(
                                    selectedEmployee.getId(),
                                    startDateTime,
                                    endDateTime
                            );
                } else {
                    shifts = AppDatabase.getInstance(this).shiftDao()
                            .getShiftsByDateRange(startDateTime, endDateTime);
                }

                // 알바생 정보 맵 생성
                Map<Long, Employee> employeeMap = new HashMap<>();
                for (Employee employee : workers) {
                    employeeMap.put(employee.getId(), employee);
                }

                runOnUiThread(() -> {
                    adapter.setShifts(shifts, employeeMap);
                    updateEmptyView(shifts.isEmpty());
                    updateTotalHours();
                });
            } catch (Exception e) {
                Log.e(TAG, "근무 일정 조회 중 오류 발생", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "근무 일정을 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateEmptyView(boolean isEmpty) {
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        shiftsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateTotalHours() {
        double totalHours = adapter.getTotalHours();
        totalHoursTextView.setText(String.format(Locale.KOREA, "총 근무 시간: %.1f시간", totalHours));
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