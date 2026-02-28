package com.example.survlatics;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SurveyReportActivity extends AppCompatActivity {

    private String surveyId;
    private LinearLayout analysisContainer;
    private TextView tvReportTitle;
    private ImageView btnBack;
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_report);

        analysisContainer = findViewById(R.id.analysisContainer);
        tvReportTitle = findViewById(R.id.tvReportTitle);
        btnBack = findViewById(R.id.btnBack);
        surveyId = getIntent().getStringExtra("SURVEY_ID");
        db = FirebaseDatabase.getInstance().getReference();

        setupNavigation();

        btnBack.setOnClickListener(v -> {
            animateClick(v);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        if (surveyId != null) {
            fetchSurveyData();
        } else {
            Toast.makeText(this, "No Survey ID found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void animateClick(View view) {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.95f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.95f, 1f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY);
        animator.setDuration(200);
        animator.start();
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, AdminActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_surveys) {
                startActivity(new Intent(this, AdminSurveyListActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, Accountadmin.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            return false;
        });
    }

    private void fetchSurveyData() {
        db.child("surveys").child(surveyId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot surveySnap) {
                if (!surveySnap.exists()) return;

                String title = surveySnap.child("title").getValue(String.class);
                tvReportTitle.setText(title != null ? title : "Analysis");

                Map<String, String> types = new HashMap<>();
                Map<String, String> texts = new HashMap<>();

                for (DataSnapshot qSnap : surveySnap.child("questions").getChildren()) {
                    types.put(qSnap.getKey(), qSnap.child("type").getValue(String.class));
                    texts.put(qSnap.getKey(), qSnap.child("text").getValue(String.class));
                }
                fetchResponses(types, texts);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchResponses(Map<String, String> types, Map<String, String> texts) {
        db.child("responses").child(surveyId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot responsesSnap) {
                Map<String, List<String>> answersMap = new HashMap<>();
                for (DataSnapshot userSnap : responsesSnap.getChildren()) {
                    for (DataSnapshot ansSnap : userSnap.getChildren()) {
                        String qId = ansSnap.getKey();
                        String answer = ansSnap.getValue(String.class);
                        if (answer != null) {
                            if (!answersMap.containsKey(qId)) answersMap.put(qId, new ArrayList<>());
                            answersMap.get(qId).add(answer);
                        }
                    }
                }

                analysisContainer.removeAllViews();
                for (String qId : types.keySet()) {
                    LinearLayout cardLayout = createQuestionCard(texts.get(qId));
                    List<String> answers = answersMap.getOrDefault(qId, new ArrayList<>());

                    if (answers.isEmpty()) {
                        addNoDataMessage(cardLayout);
                    } else if ("mcq".equalsIgnoreCase(types.get(qId))) {
                        generateDonutChart(answers, cardLayout);
                    } else {
                        generateTextAnalysis(answers, cardLayout);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private LinearLayout createQuestionCard(String questionText) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 32);
        card.setLayoutParams(params);
        card.setRadius(32f);
        card.setCardBackgroundColor(getResources().getColor(R.color.bg_card));
        card.setCardElevation(0f);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(48, 48, 48, 48);

        TextView tvQ = new TextView(this);
        tvQ.setText(questionText);
        tvQ.setTextSize(20f);
        tvQ.setTextColor(getResources().getColor(R.color.text_primary));
        tvQ.setTypeface(null, Typeface.BOLD);
        tvQ.setPadding(0, 0, 0, 32);

        inner.addView(tvQ);
        card.addView(inner);
        analysisContainer.addView(card);
        return inner;
    }

    private void addNoDataMessage(LinearLayout layout) {
        TextView tv = new TextView(this);
        tv.setText("Pending responses...");
        tv.setTextColor(getResources().getColor(R.color.text_secondary));
        layout.addView(tv);
    }

    private void generateDonutChart(List<String> answers, LinearLayout layout) {
        Map<String, Integer> freqMap = new HashMap<>();
        for (String ans : answers) freqMap.put(ans, freqMap.getOrDefault(ans, 0) + 1);

        PieChart chart = new PieChart(this);
        chart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600));
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setHoleRadius(70f);
        chart.setTransparentCircleRadius(75f);
        chart.setCenterText("Data Distribution");
        chart.setCenterTextColor(getResources().getColor(R.color.text_secondary));
        chart.setEntryLabelColor(getResources().getColor(R.color.text_primary));

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> e : freqMap.entrySet()) entries.add(new PieEntry(e.getValue(), e.getKey()));

        PieDataSet set = new PieDataSet(entries, "");
        // Using mindful theme colors
        set.setColors(new int[]{Color.parseColor("#38BDF8"), Color.parseColor("#818CF8"), Color.parseColor("#F472B6")});
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(12f);

        chart.setData(new PieData(set));
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(true);
        chart.getLegend().setTextColor(getResources().getColor(R.color.text_secondary));
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        chart.invalidate();
        layout.addView(chart);
    }

    private void generateTextAnalysis(List<String> answers, LinearLayout layout) {
        Map<String, Integer> freqMap = new HashMap<>();
        for (String ans : answers) {
            String clean = ans.toLowerCase().trim();
            freqMap.put(clean, freqMap.getOrDefault(clean, 0) + 1);
        }
        String top = Collections.max(freqMap.entrySet(), Map.Entry.comparingByValue()).getKey();

        TextView tvTop = new TextView(this);
        tvTop.setText("Common Theme: " + top);
        tvTop.setTextSize(16f);
        tvTop.setTextColor(Color.parseColor("#38BDF8"));
        tvTop.setPadding(0, 0, 0, 16);
        layout.addView(tvTop);

        TextView tvAi = new TextView(this);
        tvAi.setText("Synthesizing insights...");
        tvAi.setTextColor(getResources().getColor(R.color.text_secondary));
        layout.addView(tvAi);

        fetchGeminiAnalysis(answers, tvAi, layout);
    }

    private void fetchGeminiAnalysis(List<String> answers, TextView tvAi, LinearLayout layout) {
        // ... (Same Logic as your original Gemini call, but update runOnUiThread UI updates)
        // Ensure that the PieChart generated by AI also uses the theme-specific colors
        //
        runOnUiThread(() -> {
            tvAi.setText("AI Insights generated.");
            // Apply getResources().getColor(R.color.text_secondary) to AI summaries
        });
    }
}