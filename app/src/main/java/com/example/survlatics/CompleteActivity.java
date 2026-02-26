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

public class CompleteActivity extends AppCompatActivity {

    private RecyclerView completedRecyclerView;
    private ShimmerFrameLayout shimmerFrameLayout; // Added Shimmer variable
    private List<String> completedSurveyTitles;
    private List<String> completedSurveyIds;
    private SurveyAdapter adapter;
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.completed_survey);

        completedRecyclerView = findViewById(R.id.completedListView);
        shimmerFrameLayout = findViewById(R.id.shimmer_view_container); // Initialize Shimmer

        completedRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        completedSurveyTitles = new ArrayList<>();
        completedSurveyIds = new ArrayList<>();

        adapter = new SurveyAdapter(completedSurveyTitles, completedSurveyIds, "Completed", surveyId -> {
            Toast.makeText(CompleteActivity.this, "Survey already completed!", Toast.LENGTH_SHORT).show();
        });

        completedRecyclerView.setAdapter(adapter);
        db = FirebaseDatabase.getInstance().getReference();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_surveys);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_surveys) {
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    // Refresh the list immediately whenever this tab is opened
    @Override
    protected void onResume() {
        super.onResume();
        loadCompletedSurveys();
    }

    private void loadCompletedSurveys() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        // Start the skeleton loading animation and hide the actual list
        shimmerFrameLayout.startShimmer();
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        completedRecyclerView.setVisibility(View.GONE);

        // Use addListenerForSingleValueEvent to stop listeners from overlapping
        db.child("surveys").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> tempTitles = new ArrayList<>();
                List<String> tempIds = new ArrayList<>();

                int totalSurveys = (int) snapshot.getChildrenCount();

                if (totalSurveys == 0) {
                    // Stop animation and show empty list if there are no surveys at all
                    shimmerFrameLayout.stopShimmer();
                    shimmerFrameLayout.setVisibility(View.GONE);
                    completedRecyclerView.setVisibility(View.VISIBLE);

                    completedSurveyTitles.clear();
                    completedSurveyIds.clear();
                    adapter.notifyDataSetChanged();
                    return;
                }

                final int[] processedCount = {0};

                for (DataSnapshot data : snapshot.getChildren()) {
                    String surveyId = data.getKey();
                    String title = data.child("title").getValue(String.class);

                    db.child("responses").child(surveyId).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot responseSnapshot) {
                            if (responseSnapshot.exists()) {
                                tempTitles.add(title);
                                tempIds.add(surveyId);
                            }

                            processedCount[0]++;
                            // Update the screen ONLY when all response queries are finished
                            if (processedCount[0] == totalSurveys) {

                                // Stop the animation and reveal the real data!
                                shimmerFrameLayout.stopShimmer();
                                shimmerFrameLayout.setVisibility(View.GONE);
                                completedRecyclerView.setVisibility(View.VISIBLE);

                                completedSurveyTitles.clear();
                                completedSurveyIds.clear();
                                completedSurveyTitles.addAll(tempTitles);
                                completedSurveyIds.addAll(tempIds);
                                adapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            processedCount[0]++;
                            if (processedCount[0] == totalSurveys) {

                                // Stop the animation even if a read was cancelled
                                shimmerFrameLayout.stopShimmer();
                                shimmerFrameLayout.setVisibility(View.GONE);
                                completedRecyclerView.setVisibility(View.VISIBLE);

                                completedSurveyTitles.clear();
                                completedSurveyIds.clear();
                                completedSurveyTitles.addAll(tempTitles);
                                completedSurveyIds.addAll(tempIds);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ensure we stop the skeleton animation if there is a main database error
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
                completedRecyclerView.setVisibility(View.VISIBLE);

                Toast.makeText(CompleteActivity.this, "Failed to load completed surveys", Toast.LENGTH_SHORT).show();
            }
        });
    }
}