package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CompleteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.completed_survey);

        // Bottom bar buttons

        // Home button (optional - already on home)
        // ... inside onCreate ...
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

// Set Surveys as selected
        bottomNavigationView.setSelectedItemId(R.id.nav_surveys);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_surveys) {
                return true; // Do nothing
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }
}
