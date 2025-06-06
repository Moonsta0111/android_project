package com.example.alba_pay_manager;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.alba_pay_manager.data.Employee;
import com.example.alba_pay_manager.ui.LoginActivity;
import com.example.alba_pay_manager.util.AuthManager;
import com.example.alba_pay_manager.ui.AddWorkerActivity;
import com.example.alba_pay_manager.ui.ManageWorkersActivity;
import com.example.alba_pay_manager.ui.InputShiftActivity;
import com.example.alba_pay_manager.ui.ViewShiftsActivity;
import com.example.alba_pay_manager.ui.CalculatePayrollActivity;
import com.example.alba_pay_manager.ui.ViewPayrollActivity;
import com.example.alba_pay_manager.ui.CalendarViewActivity;

public class MainActivity extends AppCompatActivity {
    private TextView userNameTextView;
    private TextView userRoleTextView;
    private LinearLayout ownerMenuLayout;
    private LinearLayout workerMenuLayout;
    private AuthManager authManager;

    private static final int REQUEST_ADD_WORKER = 1;
    private static final int REQUEST_EDIT_WORKER = 2;
    private static final int REQUEST_INPUT_SHIFT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 초기화
        authManager = new AuthManager(this);

        initializeViews();
        setupListeners();
        updateUI();
    }

    private void initializeViews() {
        userNameTextView = findViewById(R.id.userNameTextView);
        userRoleTextView = findViewById(R.id.userRoleTextView);
        ownerMenuLayout = findViewById(R.id.ownerMenuLayout);
        workerMenuLayout = findViewById(R.id.workerMenuLayout);
    }

    private void setupListeners() {
        // 관리자용 메뉴 버튼
        findViewById(R.id.addWorkerButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddWorkerActivity.class);
            startActivityForResult(intent, REQUEST_ADD_WORKER);
        });

        findViewById(R.id.manageWorkersButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageWorkersActivity.class);
            startActivityForResult(intent, REQUEST_EDIT_WORKER);
        });

        findViewById(R.id.addShiftButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, InputShiftActivity.class);
            startActivityForResult(intent, REQUEST_INPUT_SHIFT);
        });

        findViewById(R.id.viewShiftsButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewShiftsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.calculatePayrollButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, CalculatePayrollActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.viewPayrollsButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewPayrollActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.ownerCalendarViewButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, CalendarViewActivity.class);
            startActivity(intent);
        });

        // 알바생용 메뉴 버튼
        findViewById(R.id.viewMyShiftsButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewShiftsActivity.class);
            intent.putExtra("employeeId", authManager.getCurrentUser().getId());
            startActivity(intent);
        });

        findViewById(R.id.viewMyPayrollButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewPayrollActivity.class);
            intent.putExtra("employeeId", authManager.getCurrentUser().getId());
            startActivity(intent);
        });

        findViewById(R.id.workerCalendarViewButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, CalendarViewActivity.class);
            intent.putExtra("employeeId", authManager.getCurrentUser().getId());
            startActivity(intent);
        });
    }

    private void updateUI() {
        Employee currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            userNameTextView.setText(currentUser.getName());
            userRoleTextView.setText(currentUser.getRole().equals("OWNER") ? "관리자" : "알바생");
            
            // 권한에 따라 메뉴 표시
            ownerMenuLayout.setVisibility(currentUser.getRole().equals("OWNER") ? View.VISIBLE : View.GONE);
            workerMenuLayout.setVisibility(currentUser.getRole().equals("WORKER") ? View.VISIBLE : View.GONE);
        } else {
            // 로그인 화면으로 이동
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            authManager.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            updateUI();
        }
    }
}