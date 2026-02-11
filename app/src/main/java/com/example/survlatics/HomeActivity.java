package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

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
            ListView listView = findViewById(R.id.listview);

            String[] surveys = {
                    "Customer Feedback",
                    "Product Review",
                    "Service Rating",
                    "App Experience"
            };

            ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(this,
                            android.R.layout.simple_list_item_1,
                            surveys);

            listView.setAdapter(adapter);
        });
    }
}
