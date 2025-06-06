package com.example.alba_pay_manager.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface EmployeeDao {
    @Query("SELECT * FROM Employee")
    List<Employee> getAllEmployees();

    @Query("SELECT * FROM Employee WHERE id = :id")
    Employee getEmployeeById(long id);

    @Query("SELECT * FROM Employee WHERE username = :username")
    Employee getEmployeeByUsername(String username);

    @Insert
    long insert(Employee employee);

    @Update
    void update(Employee employee);

    @Delete
    void delete(Employee employee);

    @Query("SELECT * FROM Employee WHERE role = 'WORKER'")
    List<Employee> getAllWorkers();

    @Query("SELECT * FROM Employee WHERE role = 'WORKER' ORDER BY name")
    @NonNull
    List<Employee> getAllWorkersOrdered();

    @Query("SELECT COUNT(*) FROM Employee WHERE role = 'WORKER'")
    int getWorkerCount();

    @Query("SELECT * FROM Employee WHERE role = 'OWNER' LIMIT 1")
    @Nullable
    Employee getOwner();

    @Query("SELECT * FROM Employee WHERE role = 'WORKER' AND id = :id")
    @Nullable
    Employee getWorkerById(long id);

    @Query("DELETE FROM Employee WHERE id = :id")
    void deleteById(long id);
} 