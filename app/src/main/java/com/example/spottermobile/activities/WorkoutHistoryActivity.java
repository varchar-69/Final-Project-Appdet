package com.example.spottermobile.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spottermobile.R;
import com.example.spottermobile.database.FirestoreHelper;
import com.example.spottermobile.model.WorkoutHistory;

import java.util.ArrayList;
import java.util.List;

public class WorkoutHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerHistory;
    private LinearLayout layoutEmpty;

    private TextView tvTotalSessions;
    private TextView tvTotalMinutes;
    private TextView tvTotalCalories;

    private FirestoreHelper firestoreHelper;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_history);

        setupBottomNav("workouts");

        firestoreHelper = new FirestoreHelper();

        userId = getSharedPreferences(
                "SpotterPrefs",
                MODE_PRIVATE
        ).getString("user_id", null);

        initViews();

        recyclerHistory.setLayoutManager(
                new LinearLayoutManager(this)
        );

        loadHistory();
    }

    private void initViews() {

        recyclerHistory = findViewById(R.id.recyclerWorkoutHistory);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        tvTotalSessions = findViewById(R.id.tvTotalSessions);
        tvTotalMinutes = findViewById(R.id.tvTotalMinutes);
        tvTotalCalories = findViewById(R.id.tvTotalCalories);
    }

    private void loadHistory() {

        if (userId == null || userId.isEmpty()) {
            showEmptyState();
            return;
        }

        firestoreHelper.getWorkoutHistory(
                userId,
                new FirestoreHelper.FirestoreCallback<List<WorkoutHistory>>() {

                    @Override
                    public void onSuccess(List<WorkoutHistory> history) {

                        if (history == null) {
                            history = new ArrayList<>();
                        }

                        updateStatistics(history);

                        if (history.isEmpty()) {
                            showEmptyState();
                            return;
                        }

                        layoutEmpty.setVisibility(View.GONE);
                        recyclerHistory.setVisibility(View.VISIBLE);

                        recyclerHistory.setAdapter(
                                new HistoryAdapter(history)
                        );
                    }

                    @Override
                    public void onFailure(String errorMessage) {

                        resetStatistics();
                        showEmptyState();
                    }
                });
    }

    private void updateStatistics(List<WorkoutHistory> history) {

        int totalMinutes = 0;
        int totalCalories = 0;

        for (WorkoutHistory workout : history) {

            totalMinutes += workout.getDuration();
            totalCalories += workout.getCalories();
        }

        tvTotalSessions.setText(
                String.valueOf(history.size())
        );

        tvTotalMinutes.setText(
                String.valueOf(totalMinutes)
        );

        tvTotalCalories.setText(
                String.valueOf(totalCalories)
        );
    }

    private void resetStatistics() {

        tvTotalSessions.setText("0");
        tvTotalMinutes.setText("0");
        tvTotalCalories.setText("0");
    }

    private void showEmptyState() {

        layoutEmpty.setVisibility(View.VISIBLE);
        recyclerHistory.setVisibility(View.GONE);
    }

    // ---------------- BOTTOM NAVIGATION ----------------

    private void setupBottomNav(String activeTab) {

        LinearLayout tabBook = findViewById(R.id.tabBook);
        LinearLayout tabMyBookings = findViewById(R.id.tabMyBookings);
        LinearLayout tabWorkouts = findViewById(R.id.tabWorkouts);
        LinearLayout tabBmi = findViewById(R.id.tabBmi);
        LinearLayout tabProfile = findViewById(R.id.tabProfile);

        highlightTab(tabBook, "book".equals(activeTab));
        highlightTab(tabMyBookings, "mybookings".equals(activeTab));
        highlightTab(tabWorkouts, "workouts".equals(activeTab));
        highlightTab(tabBmi, "bmi".equals(activeTab));
        highlightTab(tabProfile, "profile".equals(activeTab));

        tabBook.setOnClickListener(v ->
                navigateToTab(activeTab,
                        "book",
                        BookingActivity.class));

        tabMyBookings.setOnClickListener(v ->
                navigateToTab(activeTab,
                        "mybookings",
                        BookingHistoryActivity.class));

        tabWorkouts.setOnClickListener(v ->
                navigateToTab(activeTab,
                        "workouts",
                        WorkoutHistoryActivity.class));

        tabBmi.setOnClickListener(v ->
                navigateToTab(activeTab,
                        "bmi",
                        BMIActivity.class));

        tabProfile.setOnClickListener(v ->
                navigateToTab(activeTab,
                        "profile",
                        ProfileActivity.class));
    }

    private void navigateToTab(
            String activeTab,
            String targetTab,
            Class<?> activityClass
    ) {

        if (targetTab.equals(activeTab)) {
            return;
        }

        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
        finish();
    }

    private void highlightTab(
            LinearLayout tab,
            boolean active
    ) {

        int color = Color.parseColor(
                active ? "#FFFFFF" : "#6B7280"
        );

        for (int i = 0; i < tab.getChildCount(); i++) {

            View child = tab.getChildAt(i);

            if (child instanceof TextView) {

                ((TextView) child).setTextColor(color);

            } else if (child instanceof ImageView) {

                ((ImageView) child).setColorFilter(color);
            }
        }
    }

    // ---------------- RECYCLER VIEW ADAPTER ----------------

    private static class HistoryAdapter
            extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

        private final List<WorkoutHistory> items;

        HistoryAdapter(List<WorkoutHistory> items) {
            this.items = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(
                ViewGroup parent,
                int viewType
        ) {

            View view = LayoutInflater.from(
                    parent.getContext()
            ).inflate(
                    R.layout.item_workout_history,
                    parent,
                    false
            );

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                ViewHolder holder,
                int position
        ) {

            WorkoutHistory workout = items.get(position);

            holder.tvWorkout.setText(
                    safeText(workout.getWorkoutName(),
                            "Gym Session")
            );

            holder.tvDate.setText(
                    safeText(workout.getDate(), "—")
            );

            holder.tvDuration.setText(
                    workout.getDuration() > 0
                            ? workout.getDuration() + " min"
                            : "—"
            );

            holder.tvCalories.setText(
                    workout.getCalories() > 0
                            ? workout.getCalories() + " cal"
                            : "—"
            );
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private static String safeText(
                String value,
                String fallback
        ) {

            return value != null && !value.isEmpty()
                    ? value
                    : fallback;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvWorkout;
            TextView tvDate;
            TextView tvDuration;
            TextView tvCalories;

            ViewHolder(View view) {
                super(view);

                tvWorkout = view.findViewById(
                        R.id.tvHistoryWorkout
                );

                tvDate = view.findViewById(
                        R.id.tvHistoryDate
                );

                tvDuration = view.findViewById(
                        R.id.tvHistoryDuration
                );

                tvCalories = view.findViewById(
                        R.id.tvHistoryCalories
                );
            }
        }
    }
}