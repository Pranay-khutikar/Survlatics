package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class CompleteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.completed_survey);

        // Bottom bar buttons
        ImageButton btnHome = findViewById(R.id.imageButton);
        ImageButton btnMiddle = findViewById(R.id.imageButton2);
        ImageButton btnAccount = findViewById(R.id.imageButton3);

        // Home button (optional - already on home)
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(CompleteActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        // Middle button (optional)
        btnMiddle.setOnClickListener(v -> {
            // You can open Surveys screen later
        });

        // ✅ Account button → Account screen
        btnAccount.setOnClickListener(v -> {
            Intent intent = new Intent(CompleteActivity.this, AccountActivity.class);
            startActivity(intent);
        });
    }
}
