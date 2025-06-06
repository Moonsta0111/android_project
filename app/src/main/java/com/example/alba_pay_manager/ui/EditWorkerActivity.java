package com.example.alba_pay_manager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.alba_pay_manager.R;
import com.example.alba_pay_manager.data.AppDatabase;
import com.example.alba_pay_manager.data.Employee;
import com.example.alba_pay_manager.data.MinimumWageDao;
import com.example.alba_pay_manager.util.AuthManager;
import com.example.alba_pay_manager.util.ValidationUtils;
import com.example.alba_pay_manager.util.WageUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditWorkerActivity extends AppCompatActivity {
    private static final String TAG = "EditWorkerActivity";
    private static final String EXTRA_WORKER_ID = "worker_id";

    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout nameLayout;
    private TextInputLayout hourlyWageLayout;
    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText nameEditText;
    private TextInputEditText hourlyWageEditText;
    private TextView errorTextView;
    private Button saveButton;

    private AuthManager authManager;
    private ExecutorService executorService;
    private Employee worker;
    private MinimumWageDao minimumWageDao;

    public static Intent newIntent(android.content.Context context, long workerId) {
        Intent intent = new Intent(context, EditWorkerActivity.class);
        intent.putExtra(EXTRA_WORKER_ID, workerId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_worker);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("알바생 정보 수정");

        // 초기화
        executorService = Executors.newSingleThreadExecutor();
        authManager = new AuthManager(this);
        minimumWageDao = AppDatabase.getInstance(this).minimumWageDao();

        // 관리자 권한 확인
        if (!authManager.isOwner()) {
            Toast.makeText(this, "권한이 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 알바생 ID 가져오기
        long workerId = getIntent().getLongExtra(EXTRA_WORKER_ID, -1);
        if (workerId == -1) {
            Toast.makeText(this, "잘못된 알바생 정보입니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadWorkerData(workerId);
        setupListeners();
    }

    private void initializeViews() {
        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        nameLayout = findViewById(R.id.nameLayout);
        hourlyWageLayout = findViewById(R.id.hourlyWageLayout);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        nameEditText = findViewById(R.id.nameEditText);
        hourlyWageEditText = findViewById(R.id.hourlyWageEditText);
        errorTextView = findViewById(R.id.errorTextView);
        saveButton = findViewById(R.id.saveButton);
    }

    private void loadWorkerData(long workerId) {
        executorService.execute(() -> {
            try {
                worker = AppDatabase.getInstance(this).employeeDao().getEmployeeById(workerId);
                if (worker == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "알바생 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                runOnUiThread(() -> {
                    usernameEditText.setText(worker.getUsername());
                    nameEditText.setText(worker.getName());
                    hourlyWageEditText.setText(String.valueOf(worker.getHourlyWage()));
                });
            } catch (Exception e) {
                Log.e(TAG, "알바생 정보 로드 중 오류 발생", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "알바생 정보를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void setupListeners() {
        saveButton.setOnClickListener(v -> {
            saveButton.setEnabled(false);  // 중복 클릭 방지
            attemptSaveWorker();
        });

        // 입력 필드 포커스 변경 시 에러 메시지 초기화
        nameEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                nameLayout.setError(null);
            }
        });

        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                passwordLayout.setError(null);
            }
        });

        hourlyWageEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hourlyWageLayout.setError(null);
                validateHourlyWage();
            }
        });
    }

    private void validateHourlyWage() {
        String input = hourlyWageEditText.getText().toString().trim();
        if (input.isEmpty()) return;

        try {
            int wage = Integer.parseInt(input);
            int currentYear = java.time.Year.now().getValue();
            
            executorService.execute(() -> {
                int minWage = WageUtils.getMinimumWage(currentYear, minimumWageDao);
                String errorMessage = ValidationUtils.validateHourlyWage(wage, minWage);
                
                if (!errorMessage.isEmpty()) {
                    runOnUiThread(() -> {
                        hourlyWageLayout.setError(errorMessage);
                    });
                }
            });
        } catch (NumberFormatException e) {
            hourlyWageLayout.setError("올바른 시급을 입력해주세요.");
        }
    }

    private void attemptSaveWorker() {
        // 입력값 가져오기
        String name = nameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String hourlyWageStr = hourlyWageEditText.getText().toString().trim();

        // 에러 메시지 초기화
        errorTextView.setVisibility(View.GONE);
        nameLayout.setError(null);
        passwordLayout.setError(null);
        hourlyWageLayout.setError(null);

        // 입력값 검증
        if (name.isEmpty()) {
            nameLayout.setError("이름을 입력해주세요.");
            saveButton.setEnabled(true);
            return;
        }

        if (!password.isEmpty() && password.length() < 6) {
            passwordLayout.setError("비밀번호는 6자 이상이어야 합니다.");
            saveButton.setEnabled(true);
            return;
        }

        if (hourlyWageStr.isEmpty()) {
            hourlyWageLayout.setError("시급을 입력해주세요.");
            saveButton.setEnabled(true);
            return;
        }

        int hourlyWage;
        try {
            hourlyWage = Integer.parseInt(hourlyWageStr);
            if (hourlyWage < 0) {
                hourlyWageLayout.setError("시급은 0원 이상이어야 합니다.");
                saveButton.setEnabled(true);
                return;
            }

            // 최저시급 검증
            int currentYear = java.time.Year.now().getValue();
            int minWage = WageUtils.getMinimumWage(currentYear, minimumWageDao);
            String errorMessage = ValidationUtils.validateHourlyWage(hourlyWage, minWage);
            if (!errorMessage.isEmpty()) {
                hourlyWageLayout.setError(errorMessage);
                saveButton.setEnabled(true);
                return;
            }
        } catch (NumberFormatException e) {
            hourlyWageLayout.setError("올바른 시급을 입력해주세요.");
            saveButton.setEnabled(true);
            return;
        }

        // 알바생 정보 업데이트 (비동기 처리)
        executorService.execute(() -> {
            try {
                // 비밀번호가 입력된 경우에만 변경
                if (!password.isEmpty()) {
                    worker.setPassword(password);
                }
                worker.setName(name);
                worker.setHourlyWage(hourlyWage);

                AppDatabase.getInstance(this).employeeDao().update(worker);

                runOnUiThread(() -> {
                    Toast.makeText(this, "알바생 정보가 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                Log.e(TAG, "알바생 정보 수정 중 오류 발생", e);
                runOnUiThread(() -> {
                    errorTextView.setText("알바생 정보 수정 중 오류가 발생했습니다.");
                    errorTextView.setVisibility(View.VISIBLE);
                    saveButton.setEnabled(true);
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