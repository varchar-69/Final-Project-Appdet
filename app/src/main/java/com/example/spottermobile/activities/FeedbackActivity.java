package com.example.spottermobile.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Feedback;

public class FeedbackActivity extends AppCompatActivity {
    private RatingBar ratingBar;
    private DatabaseHelper dbHelper;
    private int userId = 1; // Simplified

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        ratingBar = findViewById(R.id.ratingBar);
        dbHelper = new DatabaseHelper(this);

        Button btnSubmitFeedback = findViewById(R.id.btnSubmitFeedback);
        btnSubmitFeedback.setOnClickListener(v -> showFeedbackDialog());
    }

    private void showFeedbackDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_feedback, null);
        builder.setView(dialogView);

            EditText etComment = dialogView.findViewById(R.id.etComment);
            RatingBar dialogRatingBar = dialogView.findViewById(R.id.dialogRatingBar);
        dialogRatingBar.setRating(ratingBar.getRating());

        builder.setPositiveButton("SUBMIT", (dialog, which) -> {
            float rating = dialogRatingBar.getRating();
            String comment = etComment.getText().toString().trim();

            if (rating > 0 && !comment.isEmpty()) {
                Feedback feedback = new Feedback(userId, rating, comment);
                if (dbHelper.addFeedback(feedback)) {
                    Toast.makeText(this, "Feedback submitted! Thank you! ⭐", Toast.LENGTH_LONG).show();
                    ratingBar.setRating(rating);
                }
            } else {
                Toast.makeText(this, "Please provide rating and comment", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }
}