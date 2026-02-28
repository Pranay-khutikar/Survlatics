package com.example.survlatics;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
    private ShimmerFrameLayout shimmerFrameLayout;
    private List<String> surveyTitles;
    private List<String> surveyIds;
    private SurveyAdapter adapter;
    private DatabaseReference db;

    // Empty State Views
    private WebView splineWebViewEmpty;
    private View layoutEmptyState; // The container for both 3D and Text

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        // --- 1. Initialize Standard Views ---
        listView = findViewById(R.id.completedListView);
        shimmerFrameLayout = findViewById(R.id.shimmer_view_container);
        FloatingActionButton fabChatbot = findViewById(R.id.fabChatbot);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // --- 2. Initialize Empty State Views ---
        splineWebViewEmpty = findViewById(R.id.spline_webview_empty);
        layoutEmptyState = findViewById(R.id.layout_empty_state); // Matches the new XML ID
        setupEmptyState3D();

        // --- 3. Chatbot setup ---
        fabChatbot.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ChatbotActivity.class));
        });

        // --- 4. Recycler View setup ---
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

        // --- 5. Bottom Navigation setup ---
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

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void setupEmptyState3D() {
        if (splineWebViewEmpty != null) {
            splineWebViewEmpty.clearCache(true);
            WebSettings webSettings = splineWebViewEmpty.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

            splineWebViewEmpty.setBackgroundColor(Color.TRANSPARENT);
            splineWebViewEmpty.setVerticalScrollBarEnabled(false);
            splineWebViewEmpty.setHorizontalScrollBarEnabled(false);
            splineWebViewEmpty.setWebViewClient(new WebViewClient());

            // Prevent scroll stealing for smoother 3D interaction
            splineWebViewEmpty.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });

            // The specific "Empty Room" model link
            splineWebViewEmpty.loadUrl("https://my.spline.design/roomrelaxingcopycopycopy-vgdkKvtoZ7eR0cWdBYX35U2f/");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSurveys();
    }

    private void loadSurveys() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        // Reset UI: Start Shimmer and hide both Data list and Empty state
        shimmerFrameLayout.startShimmer();
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);

        db.child("surveys").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> tempTitles = new ArrayList<>();
                List<String> tempIds = new ArrayList<>();

                int totalActive = 0;
                for (DataSnapshot data : snapshot.getChildren()) {
                    if (Boolean.TRUE.equals(data.child("active").getValue(Boolean.class))) {
                        totalActive++;
                    }
                }

                if (totalActive == 0) {
                    showEmptyState();
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
                                if (processedCount[0] == finalTotalActive) {
                                    updateUIWithData(tempTitles, tempIds);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                processedCount[0]++;
                                if (processedCount[0] == finalTotalActive) {
                                    updateUIWithData(tempTitles, tempIds);
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
                Toast.makeText(HomeActivity.this, "Failed to load surveys", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIWithData(List<String> titles, List<String> ids) {
        if (titles.isEmpty()) {
            showEmptyState();
        } else {
            showListState(titles, ids);
        }
    }

    private void showEmptyState() {
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
        listView.setVisibility(View.GONE);

        // Show the 3D model and the "No Survey" text container
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);

        surveyTitles.clear();
        surveyIds.clear();
        adapter.notifyDataSetChanged();
    }

    private void showListState(List<String> titles, List<String> ids) {
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);

        // Hide the empty state and show the list
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);

        surveyTitles.clear();
        surveyIds.clear();
        surveyTitles.addAll(titles);
        surveyIds.addAll(ids);
        adapter.notifyDataSetChanged();
    }
}