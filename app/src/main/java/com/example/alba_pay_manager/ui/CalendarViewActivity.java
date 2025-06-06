package com.example.alba_pay_manager.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alba_pay_manager.R;
import com.example.alba_pay_manager.data.AppDatabase;
import com.example.alba_pay_manager.data.Employee;
import com.example.alba_pay_manager.data.Shift;
import com.example.alba_pay_manager.ui.adapter.ShiftAdapter;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalendarViewActivity extends AppCompatActivity {
    private static final String TAG = "CalendarViewActivity";

    private AutoCompleteTextView employeeAutoComplete;
    private Button prevMonthButton;
    private Button nextMonthButton;
    private TextView currentMonthTextView;
    private GridLayout calendarGrid;
    private CardView shiftDetailsCard;
    private TextView selectedDateTextView;
    private RecyclerView shiftsRecyclerView;
    private TextView emptyView;

    private ExecutorService executorService;
    private Calendar currentMonth;
    private SimpleDateFormat monthFormat;
    private SimpleDateFormat dateFormat;
    private List<Employee> workers;
    private Map<Long, Employee> employeeMap;
    private Map<String, List<Shift>> shiftsByDate;
    private Employee selectedEmployee;
    private Date selectedDate;
    private ShiftAdapter shiftAdapter;

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
        currentMonth = Calendar.getInstance();
        monthFormat = new SimpleDateFormat("yyyy년 MM월", Locale.KOREA);
        dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        shiftsByDate = new HashMap<>();
        workers = new ArrayList<>();
        employeeMap = new HashMap<>();

        initializeViews();
        setupRecyclerView();
        setupListeners();
        loadWorkers();
    }

    private void initializeViews() {
        employeeAutoComplete = findViewById(R.id.employeeAutoComplete);
        prevMonthButton = findViewById(R.id.prevMonthButton);
        nextMonthButton = findViewById(R.id.nextMonthButton);
        currentMonthTextView = findViewById(R.id.currentMonthTextView);
        calendarGrid = findViewById(R.id.calendarGrid);
        shiftDetailsCard = findViewById(R.id.shiftDetailsCard);
        selectedDateTextView = findViewById(R.id.selectedDateTextView);
        shiftsRecyclerView = findViewById(R.id.shiftsRecyclerView);
        emptyView = findViewById(R.id.emptyView);

        updateMonthDisplay();
    }

    private void setupRecyclerView() {
        shiftAdapter = new ShiftAdapter();
        shiftsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        shiftsRecyclerView.setAdapter(shiftAdapter);
    }

    private void setupListeners() {
        // 이전 달 버튼
        prevMonthButton.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            loadShifts();
        });

        // 다음 달 버튼
        nextMonthButton.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            loadShifts();
        });

        // 알바생 선택
        employeeAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            selectedEmployee = workers.get(position);
            loadShifts();
        });
    }

    private void updateMonthDisplay() {
        currentMonthTextView.setText(monthFormat.format(currentMonth.getTime()));
        updateCalendarGrid();
    }

    private void updateCalendarGrid() {
        // 기존 날짜 버튼 제거
        calendarGrid.removeAllViews();
        // 요일 헤더는 유지 (7개)
        for (int i = 0; i < 7; i++) {
            calendarGrid.getChildAt(i).setVisibility(View.VISIBLE);
        }

        // 이번 달의 첫 날
        Calendar firstDay = (Calendar) currentMonth.clone();
        firstDay.set(Calendar.DAY_OF_MONTH, 1);

        // 이번 달의 마지막 날
        Calendar lastDay = (Calendar) currentMonth.clone();
        lastDay.set(Calendar.DAY_OF_MONTH, lastDay.getActualMaximum(Calendar.DAY_OF_MONTH));

        // 첫 날의 요일 (0: 일요일, 6: 토요일)
        int firstDayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK) - 1;

        // 이전 달의 마지막 날짜들
        Calendar prevMonth = (Calendar) firstDay.clone();
        prevMonth.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek);
        for (int i = 0; i < firstDayOfWeek; i++) {
            addDateButton(prevMonth.getTime(), true);
            prevMonth.add(Calendar.DAY_OF_MONTH, 1);
        }

        // 이번 달 날짜들
        Calendar current = (Calendar) firstDay.clone();
        while (current.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)) {
            addDateButton(current.getTime(), false);
            current.add(Calendar.DAY_OF_MONTH, 1);
        }

        // 다음 달의 시작 날짜들
        while (calendarGrid.getChildCount() < 42) { // 6주 * 7일
            addDateButton(current.getTime(), true);
            current.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void addDateButton(Date date, boolean isOtherMonth) {
        MaterialCardView cardView = new MaterialCardView(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(4, 4, 4, 4);
        cardView.setLayoutParams(params);
        cardView.setCardElevation(2);
        cardView.setRadius(8);
        cardView.setCardBackgroundColor(getResources().getColor(
            isOtherMonth ? android.R.color.darker_gray : android.R.color.white));

        TextView textView = new TextView(this);
        textView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        textView.setGravity(View.TEXT_ALIGNMENT_CENTER);
        textView.setPadding(8, 8, 8, 8);

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        textView.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));

        // 주말 색상 설정
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SUNDAY) {
            textView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        } else if (dayOfWeek == Calendar.SATURDAY) {
            textView.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
        }

        // 근무 일정이 있는 날짜 표시
        String dateKey = dateFormat.format(date);
        if (shiftsByDate.containsKey(dateKey)) {
            cardView.setStrokeWidth(4);
            cardView.setStrokeColor(getResources().getColor(android.R.color.holo_green_light));
        }

        cardView.setOnClickListener(v -> showShiftDetails(date));
        cardView.addView(textView);
        calendarGrid.addView(cardView);
    }

    private void loadWorkers() {
        executorService.execute(() -> {
            try {
                List<Employee> loadedWorkers = AppDatabase.getInstance(this).employeeDao().getAllWorkers();
                runOnUiThread(() -> {
                    workers.clear();
                    workers.addAll(loadedWorkers);
                    employeeMap.clear();
                    for (Employee worker : workers) {
                        employeeMap.put(worker.getId(), worker);
                    }
                    
                    // 알바생 목록을 AutoCompleteTextView에 설정
                    ArrayAdapter<Employee> adapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        workers
                    );
                    employeeAutoComplete.setAdapter(adapter);
                    
                    // 초기 근무 일정 로드
                    loadShifts();
                });
            } catch (Exception e) {
                Log.e(TAG, "알바생 목록 로드 중 오류 발생", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "알바생 목록을 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadShifts() {
        executorService.execute(() -> {
            try {
                // 월의 시작일과 종료일 설정
                Calendar calendar = (Calendar) currentMonth.clone();
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                Date startDate = calendar.getTime();
                LocalDateTime startDateTime = startDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

                calendar.add(Calendar.MONTH, 1);
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                Date endDate = calendar.getTime();
                LocalDateTime endDateTime = endDate.toInstant()
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

                // 날짜별로 근무 일정 정리
                Map<String, List<Shift>> newShiftsByDate = new HashMap<>();
                for (Shift shift : shifts) {
                    Date shiftDate = toDate(shift.getStartTime());
                    String dateKey = dateFormat.format(shiftDate);
                    newShiftsByDate.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(shift);
                }

                runOnUiThread(() -> {
                    shiftsByDate.clear();
                    shiftsByDate.putAll(newShiftsByDate);
                    updateCalendarGrid();
                });
            } catch (Exception e) {
                Log.e(TAG, "근무 일정 로드 중 오류 발생", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "근무 일정을 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private void showShiftDetails(Date date) {
        selectedDate = date;
        String dateKey = dateFormat.format(date);
        List<Shift> shifts = shiftsByDate.get(dateKey);

        if (shifts != null && !shifts.isEmpty()) {
            selectedDateTextView.setText(dateKey);
            shiftAdapter.setShifts(shifts, employeeMap);
            shiftDetailsCard.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        } else {
            shiftDetailsCard.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }
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