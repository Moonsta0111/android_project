package com.example.alba_pay_manager.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alba_pay_manager.R;
import com.example.alba_pay_manager.data.Employee;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorkerAdapter extends RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder> {
    private List<Employee> workers = new ArrayList<>();
    private final OnWorkerActionListener listener;

    public interface OnWorkerActionListener {
        void onEditWorker(Employee worker);
        void onDeleteWorker(Employee worker);
    }

    public WorkerAdapter(OnWorkerActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public WorkerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_worker, parent, false);
        return new WorkerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkerViewHolder holder, int position) {
        Employee worker = workers.get(position);
        holder.bind(worker);
    }

    @Override
    public int getItemCount() {
        return workers.size();
    }

    public void setWorkers(List<Employee> workers) {
        this.workers = workers;
        notifyDataSetChanged();
    }

    class WorkerViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView usernameTextView;
        private final TextView hourlyWageTextView;
        private final ImageButton editButton;
        private final ImageButton deleteButton;

        WorkerViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            hourlyWageTextView = itemView.findViewById(R.id.hourlyWageTextView);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        void bind(Employee worker) {
            nameTextView.setText(worker.getName());
            usernameTextView.setText(worker.getUsername());
            hourlyWageTextView.setText(String.format(Locale.KOREA, "시급: %,d원", worker.getHourlyWage()));

            editButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditWorker(worker);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteWorker(worker);
                }
            });
        }
    }
} 