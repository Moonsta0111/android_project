package com.example.alba_pay_manager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alba_pay_manager.R;
import com.example.alba_pay_manager.util.AuthManager;
import com.example.alba_pay_manager.data.Employee;
import com.example.alba_pay_manager.util.ValidationUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    
    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private Button loginButton;
    private TextView errorTextView;
    private AuthManager authManager;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        executorService = Executors.newSingleThreadExecutor();
        authManager = new AuthManager(this);

        // 이미 로그인되어 있다면 메인 화면으로 이동
        if (authManager.isLoggedIn()) {
            startMainActivity();
            finish();
            return;
        }

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        errorTextView = findViewById(R.id.errorTextView);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> {
            loginButton.setEnabled(false);  // 중복 클릭 방지
            attemptLogin();
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
    }

    private void attemptLogin() {
        // 입력값 검증
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // 에러 메시지 초기화
        errorTextView.setVisibility(View.GONE);
        usernameLayout.setError(null);
        passwordLayout.setError(null);

        // 아이디 검증
        if (username.isEmpty()) {
            usernameLayout.setError("아이디를 입력해주세요.");
            loginButton.setEnabled(true);
            return;
        }

        // 비밀번호 검증
        if (password.isEmpty()) {
            passwordLayout.setError("비밀번호를 입력해주세요.");
            loginButton.setEnabled(true);
            return;
        }

        // 로그인 시도 (비동기 처리)
        executorService.execute(() -> {
            try {
                Employee employee = authManager.login(username, password);
                runOnUiThread(() -> {
                    if (employee != null) {
                        startMainActivity();
                        finish();
                    } else {
                        // 사용자 존재 여부도 백그라운드 스레드에서 확인
                        executorService.execute(() -> {
                            try {
                                boolean userExists = authManager.isUserExists(username);
                                runOnUiThread(() -> {
                                    if (userExists) {
                                        errorTextView.setText("비밀번호가 올바르지 않습니다.");
                                    } else {
                                        errorTextView.setText("존재하지 않는 아이디입니다.");
                                    }
                                    errorTextView.setVisibility(View.VISIBLE);
                                    loginButton.setEnabled(true);
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "사용자 존재 여부 확인 중 오류 발생", e);
                                runOnUiThread(() -> {
                                    errorTextView.setText("로그인 중 오류가 발생했습니다.");
                                    errorTextView.setVisibility(View.VISIBLE);
                                    loginButton.setEnabled(true);
                                });
                            }
                        });
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "로그인 중 오류 발생", e);
                runOnUiThread(() -> {
                    String errorMessage = "로그인 중 오류가 발생했습니다.";
                    if (e instanceof android.database.sqlite.SQLiteDatabaseCorruptException) {
                        errorMessage = "데이터베이스 오류가 발생했습니다. 앱을 다시 시작해주세요.";
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    loginButton.setEnabled(true);
                });
            }
        });
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 메모리 누수 방지
        usernameEditText = null;
        passwordEditText = null;
        loginButton = null;
        errorTextView = null;
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }
} 