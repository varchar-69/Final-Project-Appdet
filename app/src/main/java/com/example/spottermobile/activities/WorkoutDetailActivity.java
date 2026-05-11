package com.example.spottermobile.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.spottermobile.R;

public class WorkoutDetailActivity extends AppCompatActivity {
    private TextView tvWorkoutTitle;
    private CheckBox cbDumbbells, cbBarbell, cbBench, cbPullup, cbTreadmill, cbWeights;

    private String[] equipment = {"Dumbbells", "Barbell", "Bench Press",
            "Pull-up Bar", "Treadmill", "Free Weights"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_detail);

        String workoutName = getIntent().getStringExtra("workout_name");
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle);
        tvWorkoutTitle.setText(workoutName);

        initCheckboxes();
        findViewById(R.id.btnSavePreferences).setOnClickListener(v -> savePreferences());
    }

    private void initCheckboxes() {
        cbDumbbells = findViewById(R.id.cbDumbbells);
        cbBarbell = findViewById(R.id.cbBarbell);
        cbBench = findViewById(R.id.cbBench);
        cbPullup = findViewById(R.id.cbPullup);
        cbTreadmill = findViewById(R.id.cbTreadmill);
        cbWeights = findViewById(R.id.cbWeights);
    }

    private void savePreferences() {
        StringBuilder selected = new StringBuilder("Your equipment: ");
        if (cbDumbbells.isChecked()) selected.append("Dumbbells ");
        if (cbBarbell.isChecked()) selected.append("Barbell ");
        if (cbBench.isChecked()) selected.append("Bench ");
        if (cbPullup.isChecked()) selected.append("Pull-up ");
        if (cbTreadmill.isChecked()) selected.append("Treadmill ");
        if (cbWeights.isChecked()) selected.append("Weights ");

        Toast.makeText(this, selected.toString(), Toast.LENGTH_LONG).show();
    }
}