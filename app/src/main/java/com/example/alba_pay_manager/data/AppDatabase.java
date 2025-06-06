package com.example.alba_pay_manager.data;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 앱의 Room 데이터베이스
 */
@Database(
    entities = {
        Employee.class,
        Shift.class,
        Payroll.class,
        MinimumWage.class
    },
    version = 1,
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";
    private static volatile AppDatabase INSTANCE;
    private static final String DB_NAME = "alba.db";

    public abstract EmployeeDao employeeDao();
    public abstract ShiftDao shiftDao();
    public abstract PayrollDao payrollDao();
    public abstract MinimumWageDao minimumWageDao();

    @NonNull
    public static synchronized AppDatabase getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    DB_NAME)
                    .fallbackToDestructiveMigration() // 데이터베이스 버전이 변경되면 테이블을 재생성
                    .allowMainThreadQueries() // 테스트를 위해 임시로 메인 스레드 쿼리 허용
                    .build();

            // 초기 데이터 확인
            try (Cursor cursor = INSTANCE.getOpenHelper().getReadableDatabase().query(
                "SELECT * FROM Employee")) {
                Log.i(TAG, "Employee 테이블 데이터 확인 시작");
                int rowCount = 0;
                while (cursor.moveToNext()) {
                    rowCount++;
                    String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
                    String role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
                    Log.i(TAG, "사용자 발견: username=" + username + ", role=" + role);
                }
                if (rowCount == 0) {
                    Log.i(TAG, "Employee 테이블이 비어있습니다. 초기 데이터를 삽입합니다.");
                    // 초기 관리자 계정 생성
                    Employee owner = new Employee("admin", "admin1234", "관리자", 0, "OWNER");
                    INSTANCE.employeeDao().insert(owner);
                    Log.i(TAG, "초기 관리자 계정이 생성되었습니다.");
                } else {
                    Log.i(TAG, "Employee 테이블에 " + rowCount + "개의 데이터가 있습니다.");
                }
            }

            // 최저시급 데이터 확인
            try (Cursor cursor = INSTANCE.getOpenHelper().getReadableDatabase().query(
                "SELECT * FROM minimum_wages")) {
                Log.i(TAG, "minimum_wages 테이블 데이터 확인 시작");
                int rowCount = 0;
                while (cursor.moveToNext()) {
                    rowCount++;
                    int year = cursor.getInt(cursor.getColumnIndexOrThrow("year"));
                    int wage = cursor.getInt(cursor.getColumnIndexOrThrow("wage"));
                    Log.i(TAG, "최저시급 발견: year=" + year + ", wage=" + wage);
                }
                if (rowCount == 0) {
                    Log.i(TAG, "minimum_wages 테이블이 비어있습니다. 초기 데이터를 삽입합니다.");
                    // 2024-2025년 최저시급 데이터 삽입
                    INSTANCE.minimumWageDao().insert(new MinimumWage(2024, 9860));
                    INSTANCE.minimumWageDao().insert(new MinimumWage(2025, 10030));
                    Log.i(TAG, "최저시급 초기 데이터가 삽입되었습니다.");
                } else {
                    Log.i(TAG, "minimum_wages 테이블에 " + rowCount + "개의 데이터가 있습니다.");
                }
            }
        }
        return INSTANCE;
    }

    private static void deleteDatabase(Context context) {
        try {
            Log.i(TAG, "데이터베이스 파일 삭제 시작");
            // 데이터베이스 파일 삭제
            context.deleteDatabase(DB_NAME);
            
            // 데이터베이스 관련 파일들도 삭제
            File dbFile = context.getDatabasePath(DB_NAME);
            if (dbFile.exists()) {
                boolean deleted = dbFile.delete();
                Log.i(TAG, "데이터베이스 파일 삭제 결과: " + deleted);
            }
            
            // WAL 파일 삭제
            File walFile = new File(dbFile.getPath() + "-wal");
            if (walFile.exists()) {
                boolean deleted = walFile.delete();
                Log.i(TAG, "WAL 파일 삭제 결과: " + deleted);
            }
            
            // SHM 파일 삭제
            File shmFile = new File(dbFile.getPath() + "-shm");
            if (shmFile.exists()) {
                boolean deleted = shmFile.delete();
                Log.i(TAG, "SHM 파일 삭제 결과: " + deleted);
            }
            
            Log.i(TAG, "데이터베이스 파일 삭제 완료");
        } catch (Exception e) {
            Log.e(TAG, "데이터베이스 파일 삭제 중 오류 발생", e);
            throw new RuntimeException("데이터베이스 파일 삭제 실패", e);
        }
    }

    private static void copyDatabaseFromAsset(Context context, String dbName) {
        try {
            Log.i(TAG, "seed.sql 파일 복사 시작");
            InputStream inputStream = context.getAssets().open("seed.sql");
            File dbFile = context.getDatabasePath(dbName);
            OutputStream outputStream = new FileOutputStream(dbFile);
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            Log.i(TAG, "seed.sql 파일 복사 완료");
        } catch (IOException e) {
            Log.e(TAG, "seed.sql 파일 복사 중 오류 발생", e);
        }
    }

    /**
     * 데이터베이스 파일이 유효한지 확인
     */
    private static boolean isValidDatabase(File dbFile) {
        try {
            android.database.sqlite.SQLiteDatabase db = android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.getPath(),
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            );
            boolean isValid = db.isDatabaseIntegrityOk();
            db.close();
            return isValid;
        } catch (Exception e) {
            Log.e(TAG, "데이터베이스 유효성 검사 중 오류 발생", e);
            return false;
        }
    }
} 