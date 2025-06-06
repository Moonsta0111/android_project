package com.example.alba_pay_manager.ui;

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

public class AddWorkerActivity extends AppCompatActivity {
    private static final String TAG = "AddWorkerActivity";
    
    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout nameLayout;
    private TextInputLayout hourlyWageLayout;
    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText nameEditText;
    private TextInputEditText hourlyWageEditText;
    private TextView errorTextView;
    private Button addButton;
    
    private AuthManager authManager;
    private ExecutorService executorService;
    private MinimumWageDao minimumWageDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_worker);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("알바생 추가");

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

        initializeViews();
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
        addButton = findViewById(R.id.addButton);
    }

    private void setupListeners() {
        addButton.setOnClickListener(v -> {
            addButton.setEnabled(false);  // 중복 클릭 방지
            attemptAddWorker();
        });

        // 입력 필드 포커스 변경 시 에러 메시지 초기화
        usernameEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                usernameLayout.setError(null);
            }
        });

        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                passwordLayout.setError(null);
            }
        });

        nameEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                nameLayout.setError(null);
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

    private void attemptAddWorker() {
        // 입력값 가져오기
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String name = nameEditText.getText().toString().trim();
        String hourlyWageStr = hourlyWageEditText.getText().toString().trim();

        // 에러 메시지 초기화
        errorTextView.setVisibility(View.GONE);
        usernameLayout.setError(null);
        passwordLayout.setError(null);
        nameLayout.setError(null);
        hourlyWageLayout.setError(null);

        // 입력값 검증
        if (username.isEmpty()) {
            usernameLayout.setError("아이디를 입력해주세요.");
            addButton.setEnabled(true);
            return;
        }

        if (!ValidationUtils.isValidUsername(username)) {
            usernameLayout.setError("아이디는 3~20자의 영문, 숫자만 사용 가능합니다.");
            addButton.setEnabled(true);
            return;
        }

        if (password.isEmpty()) {
            passwordLayout.setError("비밀번호를 입력해주세요.");
            addButton.setEnabled(true);
            return;
        }

        if (password.length() < 6) {
            passwordLayout.setError("비밀번호는 6자 이상이어야 합니다.");
            addButton.setEnabled(true);
            return;
        }

        if (name.isEmpty()) {
            nameLayout.setError("이름을 입력해주세요.");
            addButton.setEnabled(true);
            return;
        }

        if (hourlyWageStr.isEmpty()) {
            hourlyWageLayout.setError("시급을 입력해주세요.");
            addButton.setEnabled(true);
            return;
        }

        int hourlyWage;
        try {
            hourlyWage = Integer.parseInt(hourlyWageStr);
            if (hourlyWage < 0) {
                hourlyWageLayout.setError("시급은 0원 이상이어야 합니다.");
                addButton.setEnabled(true);
                return;
            }

            // 최저시급 검증
            int currentYear = java.time.Year.now().getValue();
            int minWage = WageUtils.getMinimumWage(currentYear, minimumWageDao);
            String errorMessage = ValidationUtils.validateHourlyWage(hourlyWage, minWage);
            if (!errorMessage.isEmpty()) {
                hourlyWageLayout.setError(errorMessage);
                addButton.setEnabled(true);
                return;
            }
        } catch (NumberFormatException e) {
            hourlyWageLayout.setError("올바른 시급을 입력해주세요.");
            addButton.setEnabled(true);
            return;
        }

        // 아이디 중복 확인 및 알바생 추가 (비동기 처리)
        executorService.execute(() -> {
            try {
                // 아이디 중복 확인
                if (authManager.isUserExists(username)) {
                    runOnUiThread(() -> {
                        usernameLayout.setError("이미 사용 중인 아이디입니다.");
                        addButton.setEnabled(true);
                    });
                    return;
                }

                // 알바생 추가
                Employee newWorker = new Employee(username, password, name, hourlyWage, "WORKER");
                long workerId = AppDatabase.getInstance(this).employeeDao().insert(newWorker);

                if (workerId > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "알바생이 추가되었습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    throw new Exception("알바생 추가 실패");
                }
            } catch (Exception e) {
                Log.e(TAG, "알바생 추가 중 오류 발생", e);
                runOnUiThread(() -> {
                    errorTextView.setText("알바생 추가 중 오류가 발생했습니다.");
                    errorTextView.setVisibility(View.VISIBLE);
                    addButton.setEnabled(true);
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