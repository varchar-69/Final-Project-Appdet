package com.example.spottermobile.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.spottermobile.R;

public class BMIActivity extends AppCompatActivity {
    private EditText etWeight, etHeight;
    private TextView tvBMIResult, tvCategory, tvHealthAdvice;
    private Button btnCalculate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bmi);

        initViews();
        btnCalculate = findViewById(R.id.btnCalculate);
        btnCalculate.setOnClickListener(v -> calculateBMI());
    }

    private void initViews() {
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        tvBMIResult = findViewById(R.id.tvBMIResult);
        tvCategory = findViewById(R.id.tvCategory);
        tvHealthAdvice = findViewById(R.id.tvHealthAdvice);
    }

    private void calculateBMI() {
        String weightStr = etWeight.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();

        if (TextUtils.isEmpty(weightStr) || TextUtils.isEmpty(heightStr)) {
            Toast.makeText(this, "Enter weight & height", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double weight = Double.parseDouble(weightStr);
            double height = Double.parseDouble(heightStr) / 100; // cm to m

            double bmi = weight / (height * height);

            tvBMIResult.setText(String.format("BMI: %.1f", bmi));

            BMIResult result = getBMIResult(bmi);
            tvCategory.setText(result.category);
            tvCategory.setTextColor(Color.parseColor(result.color));
            tvHealthAdvice.setText(result.advice);

        } catch (Exception e) {
            Toast.makeText(this, "Enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    private BMIResult getBMIResult(double bmi) {
        if (bmi < 18.5) {
            return new BMIResult("Underweight", "#F59E0B", "Increase calorie intake with healthy foods");
        } else if (bmi < 25) {
            return new BMIResult("Normal", "#10B981", "Maintain healthy diet & exercise");
        } else if (bmi < 30) {
            return new BMIResult("Overweight", "#F59E0B", "Reduce calories, increase cardio");
        } else {
            return new BMIResult("Obese", "#EF4444", "Consult doctor, start fitness program");
        }
    }

    private static class BMIResult {
        String category, color, advice;
        BMIResult(String category, String color, String advice) {
            this.category = category;
            this.color = color;
            this.advice = advice;
        }
    }
}