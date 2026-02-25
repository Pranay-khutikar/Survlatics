package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account);

        // Bottom bar buttons
        // ... inside onCreate ...
        Button btnLogout = findViewById(R.id.btnLogout2);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        btnLogout.setOnClickListener(v -> {
            // 1. (Optional) Clear your session data here
            // Example for SharedPreferences:
            // getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply();

            // 2. Redirect to Login Activity
            Intent intent = new Intent(AccountActivity.this, MainActivity.class);

            // 3. Clear the backstack so the user can't press 'back' to return to the account page
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);

            // Show a quick confirmation
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            finish(); // Close current activity
        });
// Set Account as selected
        bottomNavigationView.setSelectedItemId(R.id.nav_account);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_surveys) {
                startActivity(new Intent(this, CompleteActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_account) {
                return true; // Do nothing
            }
            return false;
        });
    }
}
