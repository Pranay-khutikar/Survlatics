package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.view.View; // Needed for View.VISIBLE and View.GONE
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout; // The Facebook Shimmer Library
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView listView;
    private ShimmerFrameLayout shimmerFrameLayout; // Added Shimmer variable
    private List<String> surveyTitles;
    private List<String> surveyIds;
    private SurveyAdapter adapter;
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        listView = findViewById(R.id.completedListView);
        shimmerFrameLayout = findViewById(R.id.shimmer_view_container); // Initialize Shimmer

        listView.setLayoutManager(new LinearLayoutManager(this));

        surveyTitles = new ArrayList<>();
        surveyIds = new ArrayList<>();

        adapter = new SurveyAdapter(surveyTitles, surveyIds, "Pending", surveyId -> {
            Intent intent = new Intent(HomeActivity.this, TakeSurveyActivity.class);
            intent.putExtra("SURVEY_ID", surveyId);
            startActivity(intent);
        });

        listView.setAdapter(adapter);
        db = FirebaseDatabase.getInstance().getReference();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_surveys) {
                startActivity(new Intent(this, CompleteActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    // This ensures the list reloads immediately when returning from the TakeSurveyActivity
    @Override
    protected void onResume() {
        super.onResume();
        loadSurveys();
    }

    private void loadSurveys() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        // Start the skeleton loading animation and hide the actual list
        shimmerFrameLayout.startShimmer();
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);

        // Use addListenerForSingleValueEvent to stop listeners from overlapping
        db.child("surveys").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> tempTitles = new ArrayList<>();
                List<String> tempIds = new ArrayList<>();

                // Pre-count active surveys to know when processing is finished
                int totalActive = 0;
                for (DataSnapshot data : snapshot.getChildren()) {
                    if (Boolean.TRUE.equals(data.child("active").getValue(Boolean.class))) {
                        totalActive++;
                    }
                }

                if (totalActive == 0) {
                    // Stop animation and show empty list if there are no surveys at all
                    shimmerFrameLayout.stopShimmer();
                    shimmerFrameLayout.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);

                    surveyTitles.clear();
                    surveyIds.clear();
                    adapter.notifyDataSetChanged();
                    return;
                }

                final int[] processedCount = {0};
                final int finalTotalActive = totalActive;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Boolean active = data.child("active").getValue(Boolean.class);
                    if (Boolean.TRUE.equals(active)) {
                        String surveyId = data.getKey();
                        String title = data.child("title").getValue(String.class);

                        db.child("responses").child(surveyId).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot responseSnapshot) {
                                if (!responseSnapshot.exists()) {
                                    tempTitles.add(title);
                                    tempIds.add(surveyId);
                                }

                                processedCount[0]++;
                                // Batch update: ONLY refresh adapter when all checks are done
                                if (processedCount[0] == finalTotalActive) {

                                    // Stop the animation and reveal the real data!
                                    shimmerFrameLayout.stopShimmer();
                                    shimmerFrameLayout.setVisibility(View.GONE);
                                    listView.setVisibility(View.VISIBLE);

                                    surveyTitles.clear();
                                    surveyIds.clear();
                                    surveyTitles.addAll(tempTitles);
                                    surveyIds.addAll(tempIds);
                                    adapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                processedCount[0]++;
                                if (processedCount[0] == finalTotalActive) {

                                    // Stop the animation even if a read was cancelled
                                    shimmerFrameLayout.stopShimmer();
                                    shimmerFrameLayout.setVisibility(View.GONE);
                                    listView.setVisibility(View.VISIBLE);

                                    surveyTitles.clear();
                                    surveyIds.clear();
                                    surveyTitles.addAll(tempTitles);
                                    surveyIds.addAll(tempIds);
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ensure we stop the skeleton animation if there is a main database error
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);

                Toast.makeText(HomeActivity.this, "Failed to load surveys", Toast.LENGTH_SHORT).show();
            }
        });
    }
}