package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private List<String> completedSurveyTitles;
    private List<String> completedSurveyIds;
    private SurveyAdapter adapter;
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.completed_survey);

        completedRecyclerView = findViewById(R.id.completedListView);

        // RecyclerViews require a LayoutManager
        completedRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        completedSurveyTitles = new ArrayList<>();
        completedSurveyIds = new ArrayList<>();

        // Initialize the custom SurveyAdapter and pass "Completed"
        adapter = new SurveyAdapter(completedSurveyTitles, completedSurveyIds, "Completed", surveyId -> {
            // Optional: Show a message when clicking a completed survey
            Toast.makeText(CompleteActivity.this, "Survey already completed!", Toast.LENGTH_SHORT).show();
        });

        completedRecyclerView.setAdapter(adapter);

        db = FirebaseDatabase.getInstance().getReference();
        loadCompletedSurveys();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
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

    private void loadCompletedSurveys() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        db.child("surveys").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                completedSurveyTitles.clear();
                completedSurveyIds.clear();
                adapter.notifyDataSetChanged();

                for (DataSnapshot data : snapshot.getChildren()) {
                    String surveyId = data.getKey();
                    String title = data.child("title").getValue(String.class);

                    // Only look for surveys where the user's response exists
                    db.child("responses").child(surveyId).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot responseSnapshot) {
                            if (responseSnapshot.exists()) {
                                // If response exists, it means the user completed it
                                completedSurveyTitles.add(title);
                                completedSurveyIds.add(surveyId);
                                adapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompleteActivity.this, "Failed to load completed surveys", Toast.LENGTH_SHORT).show();
            }
        });
    }
}