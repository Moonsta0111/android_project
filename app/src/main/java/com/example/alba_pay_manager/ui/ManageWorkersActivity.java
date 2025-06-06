package com.example.alba_pay_manager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alba_pay_manager.R;
import com.example.alba_pay_manager.data.AppDatabase;
import com.example.alba_pay_manager.data.Employee;
import com.example.alba_pay_manager.ui.adapter.WorkerAdapter;
import com.example.alba_pay_manager.util.AuthManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManageWorkersActivity extends AppCompatActivity implements WorkerAdapter.OnWorkerActionListener {
    private static final String TAG = "ManageWorkersActivity";
    private static final int REQUEST_ADD_WORKER = 1;
    private static final int REQUEST_EDIT_WORKER = 2;

    private RecyclerView workersRecyclerView;
    private TextView emptyView;
    private WorkerAdapter adapter;
    private AuthManager authManager;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_workers);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("알바생 관리");

        // 초기화
        executorService = Executors.newSingleThreadExecutor();
        authManager = new AuthManager(this);

        // 관리자 권한 확인
        if (!authManager.isOwner()) {
            Toast.makeText(this, "권한이 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        loadWorkers();

        // 알바생 추가 버튼
        FloatingActionButton addWorkerFab = findViewById(R.id.addWorkerFab);
        addWorkerFab.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddWorkerActivity.class);
            startActivityForResult(intent, REQUEST_ADD_WORKER);
        });
    }

    private void initializeViews() {
        workersRecyclerView = findViewById(R.id.workersRecyclerView);
        emptyView = findViewById(R.id.emptyView);
    }

    private void setupRecyclerView() {
        adapter = new WorkerAdapter(this);
        workersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        workersRecyclerView.setAdapter(adapter);
    }

    private void loadWorkers() {
        executorService.execute(() -> {
            try {
                List<Employee> workers = AppDatabase.getInstance(this)
                        .employeeDao()
                        .getAllWorkers();

                runOnUiThread(() -> {
                    adapter.setWorkers(workers);
                    updateEmptyView(workers.isEmpty());
                });
            } catch (Exception e) {
                Log.e(TAG, "알바생 목록 로드 중 오류 발생", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "알바생 목록을 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateEmptyView(boolean isEmpty) {
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        workersRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onEditWorker(Employee worker) {
        Intent intent = EditWorkerActivity.newIntent(this, worker.getId());
        startActivityForResult(intent, REQUEST_EDIT_WORKER);
    }

    @Override
    public void onDeleteWorker(Employee worker) {
        new AlertDialog.Builder(this)
                .setTitle("알바생 삭제")
                .setMessage(worker.getName() + "님을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteWorker(worker))
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteWorker(Employee worker) {
        executorService.execute(() -> {
            try {
                AppDatabase.getInstance(this).employeeDao().deleteById(worker.getId());
                runOnUiThread(() -> {
                    Toast.makeText(this, "알바생이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    loadWorkers();
                });
            } catch (Exception e) {
                Log.e(TAG, "알바생 삭제 중 오류 발생", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "알바생 삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadWorkers();  // 알바생 추가/수정 후 목록 새로고침
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