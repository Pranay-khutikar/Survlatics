package com.example.survlatics;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
//import android.widget.ArrayAdapter;
import android.widget.ImageButton;
//import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        ImageButton btnHome = findViewById(R.id.imageButton);
        ImageButton btnMiddle = findViewById(R.id.imageButton2);
        ImageButton btnAccount = findViewById(R.id.imageButton3);

        btnHome.setOnClickListener(v -> {
            // already here
        });

        btnMiddle.setOnClickListener(v -> {
            startActivity(new Intent(this, CompleteActivity.class));
        });

        btnAccount.setOnClickListener(v -> {
            startActivity(new Intent(this, AccountActivity.class));
            SharedPreferences prefs = getSharedPreferences("surveys", MODE_PRIVATE);

            Set<String> completed =
                    prefs.getStringSet("completed_list", new HashSet<>());

            Set<String> newSet = new HashSet<>(completed);
            newSet.add("Customer Feedback Survey");   // survey name

            prefs.edit().putStringSet("completed_list", newSet).apply();

        });
    }
}
