package com.example.spottermobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.spottermobile.R;

public class WorkoutListActivity extends AppCompatActivity {
    private ListView listViewWorkouts;

    private String[] workouts = {
            "Chest & Triceps (45min)",
            "Legs & Glutes (60min)",
            "Back & Biceps (50min)",
            "Full Body HIIT (40min)",
            "Cardio Burn (30min)",
            "Core & Abs (35min)",
            "⚡ Upper Body Power (55min)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_list);

        listViewWorkouts = findViewById(R.id.listViewWorkouts);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, workouts);
        listViewWorkouts.setAdapter(adapter);

        listViewWorkouts.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, WorkoutDetailActivity.class);
            intent.putExtra("workout_name", workouts[position]);
            startActivity(intent);
        });
    }
}