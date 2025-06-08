package com.example.alba_pay_manager.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.CalendarView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.alba_pay_manager.R;
import com.example.alba_pay_manager.data.AppDatabase;
import com.example.alba_pay_manager.data.Employee;
import com.example.alba_pay_manager.data.Shift;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalendarViewActivity extends AppCompatActivity {
    private static final String TAG = "CalendarViewActivity";
    private CalendarView calendarView;
    private MaterialAutoCompleteTextView employeeDropdown;
    private ListView shiftListView;
    private TextView emptyView;
    private TextView selectedDateTextView;

    private ExecutorService executorService;
    private List<Employee> workers;
    private Employee selectedEmployee;
    private long selectedDateMillis;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일", Locale.KOREA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_view);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("근무 일정 캘린더");

        // 초기화
        executorService = Executors.newSingleThreadExecutor();
        selectedDateMillis = System.currentTimeMillis();

        initializeViews();
        // 파트타이머라면 드롭다운 숨김
        if (!new com.example.alba_pay_manager.util.AuthManager(this).isOwner()) {
            employeeDropdown.setVisibility(View.GONE);
        }
        loadWorkers();
        setupListeners();
    }

    private void initializeViews() {
        calendarView = findViewById(R.id.calendarView);
        employeeDropdown = findViewById(R.id.employeeDropdown);
        shiftListView = findViewById(R.id.shiftListView);
        emptyView = findViewById(R.id.emptyView);
        selectedDateTextView = findViewById(R.id.selectedDateTextView);
    }

    private void loadWorkers() {
        executorService.execute(() -> {
            try {
                workers = AppDatabase.getInstance(this).employeeDao().getAllWorkers();
                List<String> workerNames = new ArrayList<>();
                workerNames.add("전체");  // "전체" 옵션 추가
                for (Employee worker : workers) {
                    workerNames.add(worker.getName());
                }

                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            workerNames
                    );
                    employeeDropdown.setAdapter(adapter);
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
        employeeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {  // "전체" 선택
                selectedEmployee = null;
            } else {
                selectedEmployee = workers.get(position - 1);  // "전체" 옵션 때문에 인덱스 조정
            }
            loadShiftsForSelectedDate();
        });

        // 날짜 선택
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            selectedDateMillis = cal.getTimeInMillis();
            loadShiftsForSelectedDate();
        });
    }

    private void loadShiftsForSelectedDate() {
        executorService.execute(() -> {
            try {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTimeInMillis(selectedDateMillis);
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                long startMillis = cal.getTimeInMillis();
                cal.add(java.util.Calendar.DATE, 1);
                long endMillis = cal.getTimeInMillis();

                LocalDateTime startDateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(startMillis), ZoneId.systemDefault());
                LocalDateTime endDateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(endMillis), ZoneId.systemDefault());

                List<Shift> shifts;
                if (new com.example.alba_pay_manager.util.AuthManager(this).isOwner()) {
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
                } else {
                    // 파트타이머는 본인 데이터만
                    long myId = new com.example.alba_pay_manager.util.AuthManager(this).getCurrentUser().getId();
                    shifts = AppDatabase.getInstance(this).shiftDao()
                            .getShiftsByEmployeeAndDateRange(
                                    myId,
                                    startDateTime,
                                    endDateTime
                            );
                }

                runOnUiThread(() -> {
                    java.time.LocalDate selectedDate = startDateTime.toLocalDate();
                    selectedDateTextView.setText(selectedDate.format(dateFormatter));

                    if (shifts.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        shiftListView.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        shiftListView.setVisibility(View.VISIBLE);

                        List<String> shiftStrings = new ArrayList<>();
                        for (Shift shift : shifts) {
                            Employee employee = null;
                            for (Employee worker : workers) {
                                if (worker.getId() == shift.getEmployeeId()) {
                                    employee = worker;
                                    break;
                                }
                            }

                            String employeeName = employee != null ? employee.getName() : "알 수 없음";
                            String startTime = shift.getStartTime().format(
                                DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA));
                            String endTime = shift.getEndTime().format(
                                DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA));
                            shiftStrings.add(String.format("%s: %s ~ %s", employeeName, startTime, endTime));
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_list_item_1,
                            shiftStrings
                        );
                        shiftListView.setAdapter(adapter);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "근무 일정 로드 중 오류 발생", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "근무 일정을 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
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