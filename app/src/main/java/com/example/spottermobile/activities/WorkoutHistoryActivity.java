package com.example.spottermobile.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.WorkoutHistory;

import java.util.List;

public class WorkoutHistoryActivity extends AppCompatActivity {

    private RecyclerView  recyclerHistory;
    private LinearLayout  layoutEmpty;
    private TextView      tvTotalSessions;
    private TextView      tvTotalMinutes;
    private TextView      tvTotalCalories;

    private DatabaseHelper dbHelper;
    private int            userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_history);

        dbHelper = new DatabaseHelper(this);
        userId   = getSharedPreferences("SpotterPrefs", MODE_PRIVATE)
                .getInt("user_id", -1);

        recyclerHistory  = findViewById(R.id.recyclerWorkoutHistory);
        layoutEmpty      = findViewById(R.id.layoutEmpty);
        tvTotalSessions  = findViewById(R.id.tvTotalSessions);
        tvTotalMinutes   = findViewById(R.id.tvTotalMinutes);
        tvTotalCalories  = findViewById(R.id.tvTotalCalories);

        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        loadHistory();
    }

    private void loadHistory() {
        List<WorkoutHistory> history = dbHelper.getWorkoutHistory(userId);

        // Compute summary stats
        int totalMinutes  = 0;
        int totalCalories = 0;
        for (WorkoutHistory wh : history) {
            totalMinutes  += wh.getDuration();
            totalCalories += wh.getCalories();
        }

        tvTotalSessions.setText(String.valueOf(history.size()));
        tvTotalMinutes.setText(String.valueOf(totalMinutes));
        tvTotalCalories.setText(String.valueOf(totalCalories));

        if (history.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerHistory.setVisibility(View.GONE);
            return;
        }

        layoutEmpty.setVisibility(View.GONE);
        recyclerHistory.setVisibility(View.VISIBLE);
        recyclerHistory.setAdapter(new HistoryAdapter(history));
    }

    // ── ADAPTER ────────────────────────────────────────────────────────────────

    private static class HistoryAdapter
            extends RecyclerView.Adapter<HistoryAdapter.VH> {

        private final List<WorkoutHistory> items;

        HistoryAdapter(List<WorkoutHistory> items) { this.items = items; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_workout_history, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            WorkoutHistory wh = items.get(position);

            holder.tvWorkout.setText(wh.getWorkoutName() != null
                    ? wh.getWorkoutName() : "Gym Session");

            holder.tvDate.setText(wh.getDate() != null ? wh.getDate() : "—");

            holder.tvDuration.setText(wh.getDuration() > 0
                    ? wh.getDuration() + " min" : "—");

            holder.tvCalories.setText(wh.getCalories() > 0
                    ? wh.getCalories() + " cal" : "—");
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvWorkout, tvDate, tvDuration, tvCalories;
            VH(View v) {
                super(v);
                tvWorkout  = v.findViewById(R.id.tvHistoryWorkout);
                tvDate     = v.findViewById(R.id.tvHistoryDate);
                tvDuration = v.findViewById(R.id.tvHistoryDuration);
                tvCalories = v.findViewById(R.id.tvHistoryCalories);
            }
        }
    }
}
