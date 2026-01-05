package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class AdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_admin);

        // Bottom bar buttons
        ImageButton btnHome = findViewById(R.id.imageButton);
        ImageButton btnMiddle = findViewById(R.id.imageButton2);
        ImageButton btnAccount = findViewById(R.id.imageButton3);

        // Add Survey button
        Button btnAddSurvey = findViewById(R.id.btnAddSurvey);

        // Home button
        btnHome.setOnClickListener(v -> {
            // Already on home
        });

        btnMiddle.setOnClickListener(v -> {
            startActivity(new Intent(this, CompleteActivity.class));
        });

        btnAccount.setOnClickListener(v -> {
            startActivity(new Intent(this, Accountadmin.class));
        });

        // ✅ ADD SURVEY ACTION
        btnAddSurvey.setOnClickListener(v -> {
            startActivity(new Intent(this, AddSurveyActivity.class));
        });
    }
}
