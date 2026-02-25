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

public class HomeActivity extends AppCompatActivity {

    private RecyclerView listView; // Type is RecyclerView
    private List<String> surveyTitles;
    private List<String> surveyIds;
    private SurveyAdapter adapter; // Must use custom adapter, NOT ArrayAdapter
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        listView = findViewById(R.id.completedListView);

        // RecyclerView requires a LayoutManager
        listView.setLayoutManager(new LinearLayoutManager(this));

        surveyTitles = new ArrayList<>();
        surveyIds = new ArrayList<>();

        // Initialize custom adapter, pass "Pending", and handle clicks
        adapter = new SurveyAdapter(surveyTitles, surveyIds, "Pending", new SurveyAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String surveyId) {
                Intent intent = new Intent(HomeActivity.this, TakeSurveyActivity.class);
                intent.putExtra("SURVEY_ID", surveyId);
                startActivity(intent);
            }
        });

        listView.setAdapter(adapter);

        db = FirebaseDatabase.getInstance().getReference();

        loadSurveys();

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

    private void loadSurveys() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        db.child("surveys").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                surveyTitles.clear();
                surveyIds.clear();
                adapter.notifyDataSetChanged();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Boolean active = data.child("active").getValue(Boolean.class);
                    if (Boolean.TRUE.equals(active)) {
                        String surveyId = data.getKey();
                        String title = data.child("title").getValue(String.class);

                        db.child("responses").child(surveyId).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot responseSnapshot) {
                                if (!responseSnapshot.exists()) {
                                    surveyTitles.add(title);
                                    surveyIds.add(surveyId);
                                    adapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Failed to load surveys", Toast.LENGTH_SHORT).show();
            }
        });
    }
}