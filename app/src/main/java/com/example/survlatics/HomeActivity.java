package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        // Bottom bar buttons
        ImageButton btnHome = findViewById(R.id.imageButton);
        ImageButton btnMiddle = findViewById(R.id.imageButton2);
        ImageButton btnAccount = findViewById(R.id.imageButton3);

        // Home button (optional - already on home)
        btnHome.setOnClickListener(v -> {
            // Do nothing or refresh home
        });

        // Middle button (optional)
        btnMiddle.setOnClickListener(v -> {
            // You can open Surveys screen later
            Intent intent = new Intent(HomeActivity.this, CompleteActivity.class);
            startActivity(intent);
        });

        // ✅ Account button → Account screen
        btnAccount.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AccountActivity.class);
            startActivity(intent);
        });
    }
}
