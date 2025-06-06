package com.example.alba_pay_manager.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
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
import com.example.alba_pay_manager.util.AuthManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class InputShiftActivity extends AppCompatActivity {
    private static final String TAG = "InputShiftActivity";

    private TextInputLayout employeeLayout;
    private TextInputLayout dateLayout;
    private TextInputLayout startTimeLayout;
    private TextInputLayout endTimeLayout;
    private AutoCompleteTextView employeeAutoComplete;
    private TextInputEditText dateEditText;
    private TextInputEditText startTimeEditText;
    private TextInputEditText endTimeEditText;
    private TextView totalHoursTextView;
    private TextView errorTextView;
    private Button saveButton;

    private AuthManager authManager;
    private ExecutorService executorService;
    private Calendar selectedDate;
    private Calendar startTime;
    private Calendar endTime;
    private Employee selectedEmployee;
    private List<Employee> workers;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat timeFormat12Hour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_shift);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("근무 일정 입력");

        // 초기화
        executorService = Executors.newSingleThreadExecutor();
        authManager = new AuthManager(this);
        dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        timeFormat = new SimpleDateFormat("HH:mm", Locale.KOREA);
        timeFormat12Hour = new SimpleDateFormat("a hh:mm", Locale.KOREA);

        // 관리자 권한 확인
        if (!authManager.isOwner()) {
            Toast.makeText(this, "권한이 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupDateAndTime();
        loadWorkers();
        setupListeners();
    }

    private void initializeViews() {
        employeeLayout = findViewById(R.id.employeeLayout);
        dateLayout = findViewById(R.id.dateLayout);
        startTimeLayout = findViewById(R.id.startTimeLayout);
        endTimeLayout = findViewById(R.id.endTimeLayout);
        employeeAutoComplete = findViewById(R.id.employeeAutoComplete);
        dateEditText = findViewById(R.id.dateEditText);
        startTimeEditText = findViewById(R.id.startTimeEditText);
        endTimeEditText = findViewById(R.id.endTimeEditText);
        totalHoursTextView = findViewById(R.id.totalHoursTextView);
        errorTextView = findViewById(R.id.errorTextView);
        saveButton = findViewById(R.id.saveButton);
    }

    private void setupDateAndTime() {
        // 현재 날짜와 시간으로 초기화
        selectedDate = Calendar.getInstance();
        startTime = Calendar.getInstance();
        endTime = Calendar.getInstance();
        endTime.add(Calendar.HOUR_OF_DAY, 1);  // 기본 종료 시간은 1시간 후

        // 초기 날짜와 시간 표시
        updateDateAndTimeDisplay();
    }

    private void updateDateAndTimeDisplay() {
        dateEditText.setText(dateFormat.format(selectedDate.getTime()));
        startTimeEditText.setText(timeFormat12Hour.format(startTime.getTime()));
        endTimeEditText.setText(timeFormat12Hour.format(endTime.getTime()));
        updateTotalHours();
    }

    private void updateTotalHours() {
        if (startTime != null && endTime != null) {
            long diffMillis = endTime.getTimeInMillis() - startTime.getTimeInMillis();
            long totalMinutes = diffMillis / (1000 * 60);
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            totalHoursTextView.setText(String.format(Locale.KOREA, "총 근무 시간: %d시간 %d분", hours, minutes));
        }
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

        // 날짜 선택
        dateEditText.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(year, month, dayOfMonth);
                        startTime.set(year, month, dayOfMonth);
                        endTime.set(year, month, dayOfMonth);
                        updateDateAndTimeDisplay();
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });

        // 시작 시간 선택
        startTimeEditText.setOnClickListener(v -> {
            int currentHour = startTime.get(Calendar.HOUR_OF_DAY);
            int currentMinute = startTime.get(Calendar.MINUTE);
            
            // 30분 단위로 반올림
            currentMinute = (currentMinute + 15) / 30 * 30;
            if (currentMinute == 60) {
                currentHour = (currentHour + 1) % 24;
                currentMinute = 0;
            }
            
            TimePickerDialog dialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        // 30분 단위로 반올림
                        minute = (minute + 15) / 30 * 30;
                        if (minute == 60) {
                            hourOfDay = (hourOfDay + 1) % 24;
                            minute = 0;
                        }
                        
                        startTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        startTime.set(Calendar.MINUTE, minute);
                        updateDateAndTimeDisplay();
                    },
                    currentHour,
                    currentMinute,
                    false  // 12시간제 사용
            );
            dialog.show();
        });

        // 종료 시간 선택
        endTimeEditText.setOnClickListener(v -> {
            int currentHour = endTime.get(Calendar.HOUR_OF_DAY);
            int currentMinute = endTime.get(Calendar.MINUTE);
            
            // 30분 단위로 반올림
            currentMinute = (currentMinute + 15) / 30 * 30;
            if (currentMinute == 60) {
                currentHour = (currentHour + 1) % 24;
                currentMinute = 0;
            }
            
            TimePickerDialog dialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        // 30분 단위로 반올림
                        minute = (minute + 15) / 30 * 30;
                        if (minute == 60) {
                            hourOfDay = (hourOfDay + 1) % 24;
                            minute = 0;
                        }
                        
                        endTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        endTime.set(Calendar.MINUTE, minute);
                        updateDateAndTimeDisplay();
                    },
                    currentHour,
                    currentMinute,
                    false  // 12시간제 사용
            );
            dialog.show();
        });

        // 저장 버튼
        saveButton.setOnClickListener(v -> {
            saveButton.setEnabled(false);  // 중복 클릭 방지
            attemptSaveShift();
        });
    }

    private void attemptSaveShift() {
        // 에러 메시지 초기화
        errorTextView.setVisibility(View.GONE);
        employeeLayout.setError(null);
        dateLayout.setError(null);
        startTimeLayout.setError(null);
        endTimeLayout.setError(null);

        // 입력값 검증
        if (selectedEmployee == null) {
            employeeLayout.setError("알바생을 선택해주세요.");
            saveButton.setEnabled(true);
            return;
        }

        if (startTime.after(endTime)) {
            startTimeLayout.setError("시작 시간이 종료 시간보다 늦을 수 없습니다.");
            saveButton.setEnabled(true);
            return;
        }

        // 근무 시간이 24시간을 초과하는지 확인
        long diffMillis = endTime.getTimeInMillis() - startTime.getTimeInMillis();
        if (diffMillis > TimeUnit.HOURS.toMillis(24)) {
            startTimeLayout.setError("근무 시간은 24시간을 초과할 수 없습니다.");
            saveButton.setEnabled(true);
            return;
        }

        // 근무 일정 저장 (비동기 처리)
        executorService.execute(() -> {
            try {
                // 시작 시간과 종료 시간을 Date 객체로 변환
                Date startDate = startTime.getTime();
                Date endDate = endTime.getTime();

                // Date를 LocalDateTime으로 변환하는 헬퍼 메서드
                LocalDateTime startDateTime = toLocalDateTime(startDate);
                LocalDateTime endDateTime = toLocalDateTime(endDate);
                
                // 근무 일정 생성
                Shift shift = new Shift(selectedEmployee.getId(), startDateTime, endDateTime);
                long shiftId = AppDatabase.getInstance(this).shiftDao().insert(shift);

                if (shiftId > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "근무 일정이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                } else {
                    throw new Exception("근무 일정 저장 실패");
                }
            } catch (Exception e) {
                Log.e(TAG, "근무 일정 저장 중 오류 발생", e);
                runOnUiThread(() -> {
                    errorTextView.setText("근무 일정 저장 중 오류가 발생했습니다.");
                    errorTextView.setVisibility(View.VISIBLE);
                    saveButton.setEnabled(true);
                });
            }
        });
    }

    // Date를 LocalDateTime으로 변환하는 헬퍼 메서드
    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
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