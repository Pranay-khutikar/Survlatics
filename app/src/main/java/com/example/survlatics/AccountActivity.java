package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account);

        // Bottom bar buttons
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        Button btnLogout = findViewById(R.id.btnLogout2);

        // Home button (optional - already on home)
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // Handle Home click
                return true;
            } else if (id == R.id.nav_surveys) {
                // Handle Survey click
                return true;
            } else if (id == R.id.nav_account) {
                // This is where your 'nav_account' logic goes!
                startActivity(new Intent(this, AccountActivity.class));
                return true;
            }
            return false;
        });
    }
}
