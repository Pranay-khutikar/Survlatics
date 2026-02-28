package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminSurveyListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ShimmerFrameLayout shimmerFrameLayout;
    private SurveyAdapter adapter;
    private List<String> surveyTitles;
    private List<String> surveyIds;
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_survey_list);

        // 1. Initialize Views
        recyclerView = findViewById(R.id.compl_list);
        shimmerFrameLayout = findViewById(R.id.shimmer_view_container);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        surveyTitles = new ArrayList<>();
        surveyIds = new ArrayList<>();
        db = FirebaseDatabase.getInstance().getReference();

        // 2. Setup Adapter
        // The adapter already has the soft click animation built-in from our earlier changes!
        adapter = new SurveyAdapter(surveyTitles, surveyIds, "View Analysis", surveyId -> {
            Intent intent = new Intent(AdminSurveyListActivity.this, SurveyReportActivity.class);
            intent.putExtra("SURVEY_ID", surveyId);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        recyclerView.setAdapter(adapter);

        // 3. Fetch Data
        loadAllSurveys();

        // 4. Bottom Navigation Setup
        bottomNavigationView.setSelectedItemId(R.id.nav_surveys);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, AdminActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_surveys) {
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, Accountadmin.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            return false;
        });
    }

    private void loadAllSurveys() {
        // Start shimmer animation and hide the real list
        shimmerFrameLayout.startShimmer();
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        db.child("surveys").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                surveyTitles.clear();
                surveyIds.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    String title = data.child("title").getValue(String.class);
                    if (title != null) {
                        surveyTitles.add(title);
                        surveyIds.add(data.getKey());
                    }
                }

                // Stop shimmer and reveal the list
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

                Toast.makeText(AdminSurveyListActivity.this, "Failed to load surveys", Toast.LENGTH_SHORT).show();
            }
        });
    }
}