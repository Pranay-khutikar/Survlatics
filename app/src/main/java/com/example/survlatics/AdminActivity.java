package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_admin);

        // UI Components
        Button btnAddSurvey = findViewById(R.id.btnAddSurvey);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // 1. Set Home as selected icon
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        // 2. Navigation Logic
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true; // Already here
            } else if (id == R.id.nav_surveys) {
                // -> OPEN THE NEW SURVEY LIST ACTIVITY <-
                startActivity(new Intent(this, AdminSurveyListActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_account) {
                // Navigate to the Admin's Account/User Management page
                startActivity(new Intent(this, Accountadmin.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        // Add Survey Action
        btnAddSurvey.setOnClickListener(v -> {
            startActivity(new Intent(this, AddSurveyActivity.class));
        });
    }
}