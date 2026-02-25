package com.example.survlatics;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
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
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_report);

        // 1. Initialize Views
        analysisContainer = findViewById(R.id.analysisContainer);
        tvReportTitle = findViewById(R.id.tvReportTitle);
        surveyId = getIntent().getStringExtra("SURVEY_ID");
        db = FirebaseDatabase.getInstance().getReference();

        // 2. Setup Bottom Navigation (FIXED: Moved inside onCreate)
        setupNavigation();

        // 3. Load Data
        if (surveyId != null) {
            fetchSurveyData();
        } else {
            Toast.makeText(this, "Error: No Survey ID provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Set Home as selected icon
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, AdminActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_surveys) {
                startActivity(new Intent(this, AdminSurveyListActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, Accountadmin.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void fetchSurveyData() {
        db.child("surveys").child(surveyId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot surveySnap) {
                if (!surveySnap.exists()) {
                    Toast.makeText(SurveyReportActivity.this, "Survey not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                String title = surveySnap.child("title").getValue(String.class);
                tvReportTitle.setText(title != null ? title + " - Analytics" : "Survey Analytics");

                Map<String, String> questionTypes = new HashMap<>();
                Map<String, String> questionTexts = new HashMap<>();

                for (DataSnapshot qSnap : surveySnap.child("questions").getChildren()) {
                    String qId = qSnap.getKey();
                    questionTypes.put(qId, qSnap.child("type").getValue(String.class));
                    questionTexts.put(qId, qSnap.child("text").getValue(String.class));
                }

                fetchResponses(questionTypes, questionTexts);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SurveyReportActivity.this, "Failed to load survey data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchResponses(Map<String, String> questionTypes, Map<String, String> questionTexts) {
        db.child("responses").child(surveyId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot responsesSnap) {
                Map<String, List<String>> answersMap = new HashMap<>();

                for (DataSnapshot userSnap : responsesSnap.getChildren()) {
                    for (DataSnapshot ansSnap : userSnap.getChildren()) {
                        String qId = ansSnap.getKey();
                        String answer = ansSnap.getValue(String.class);

                        if (answer != null && !answer.isEmpty()) {
                            if (!answersMap.containsKey(qId)) {
                                answersMap.put(qId, new ArrayList<>());
                            }
                            answersMap.get(qId).add(answer);
                        }
                    }
                }

                analysisContainer.removeAllViews();
                addReportHeader();

                for (String qId : questionTypes.keySet()) {
                    String type = questionTypes.get(qId);
                    String qText = questionTexts.get(qId);
                    List<String> answers = answersMap.getOrDefault(qId, new ArrayList<>());

                    LinearLayout cardLayout = createQuestionCard(qText);

                    if (answers.isEmpty()) {
                        addNoDataMessage(cardLayout);
                        continue;
                    }

                    if ("mcq".equalsIgnoreCase(type)) {
                        generateDonutChart(answers, cardLayout);
                    } else if ("text".equalsIgnoreCase(type)) {
                        generateTextAnalysis(answers, cardLayout);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SurveyReportActivity.this, "Failed to load responses", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addReportHeader() {
        if (tvReportTitle.getParent() != null) {
            ((ViewGroup) tvReportTitle.getParent()).removeView(tvReportTitle);
        }
        analysisContainer.addView(tvReportTitle);
    }

    private LinearLayout createQuestionCard(String questionText) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 16, 0, 32);
        card.setLayoutParams(cardParams);
        card.setRadius(24f);
        card.setCardElevation(12f);
        card.setUseCompatPadding(true);

        LinearLayout cardInnerLayout = new LinearLayout(this);
        cardInnerLayout.setOrientation(LinearLayout.VERTICAL);
        cardInnerLayout.setPadding(40, 40, 40, 40);

        TextView tvQ = new TextView(this);
        tvQ.setText(questionText);
        tvQ.setTextSize(18f);
        tvQ.setTextColor(Color.parseColor("#212121"));
        tvQ.setTypeface(null, Typeface.BOLD);
        tvQ.setPadding(0, 0, 0, 24);

        cardInnerLayout.addView(tvQ);
        card.addView(cardInnerLayout);
        analysisContainer.addView(card);

        return cardInnerLayout;
    }

    private void addNoDataMessage(LinearLayout layout) {
        TextView tv = new TextView(this);
        tv.setText("No responses yet for this question.");
        tv.setTextColor(Color.GRAY);
        tv.setPadding(0, 0, 0, 10);
        layout.addView(tv);
    }

    private void generateDonutChart(List<String> answers, LinearLayout layout) {
        Map<String, Integer> freqMap = new HashMap<>();
        for (String ans : answers) {
            freqMap.put(ans, freqMap.getOrDefault(ans, 0) + 1);
        }

        PieChart pieChart = new PieChart(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 650);
        pieChart.setLayoutParams(params);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(58f);
        pieChart.setCenterText("MCQ\nResults");
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.GRAY);

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : freqMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setWordWrapEnabled(true);
        pieChart.invalidate();

        layout.addView(pieChart);
    }

    private void generateTextAnalysis(List<String> answers, LinearLayout layout) {
        Map<String, Integer> freqMap = new HashMap<>();
        for (String ans : answers) {
            String lower = ans.toLowerCase().trim();
            freqMap.put(lower, freqMap.getOrDefault(lower, 0) + 1);
        }
        String mostRepeated = Collections.max(freqMap.entrySet(), Map.Entry.comparingByValue()).getKey();

        TextView tvMostRepeated = new TextView(this);
        tvMostRepeated.setText("📌 Most Repeated: \"" + mostRepeated + "\"");
        tvMostRepeated.setTextSize(15f);
        tvMostRepeated.setTextColor(Color.parseColor("#00796B"));
        tvMostRepeated.setPadding(0, 10, 0, 20);
        layout.addView(tvMostRepeated);

        TextView tvSummary = new TextView(this);
        tvSummary.setText("🤖 Analyzing with AI...");
        tvSummary.setTextSize(14);
        tvSummary.setTextColor(Color.DKGRAY);
        tvSummary.setPadding(0, 10, 0, 10);
        layout.addView(tvSummary);

        fetchGeminiAnalysis(answers, tvSummary, layout);
    }

    private void fetchGeminiAnalysis(List<String> answers, TextView tvSummary, LinearLayout layout) {
        String prompt = "Analyze these survey responses: " + answers.toString() + ". " +
                "Return ONLY a valid JSON object. Required keys: " +
                "'summary' (2-sentence feedback summary), " +
                "'positive' (integer), 'negative' (integer), 'neutral' (integer). " +
                "Do not use markdown backticks.";

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + BuildConfig.GEMINI_API_KEY;

        try {
            JSONObject jsonBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();

            part.put("text", prompt);
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            jsonBody.put("contents", contents);

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder().url(url).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> tvSummary.setText("❌ AI failed: " + e.getMessage()));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (!response.isSuccessful()) {
                        Log.e("GEMINI_ERROR", "Code: " + response.code() + " Body: " + responseBody);
                        runOnUiThread(() -> tvSummary.setText("❌ API Error: " + response.code()));
                        return;
                    }

                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        String aiReply = jsonObject.getJSONArray("candidates")
                                .getJSONObject(0).getJSONObject("content")
                                .getJSONArray("parts").getJSONObject(0).getString("text");

                        aiReply = aiReply.replace("```json", "").replace("```", "").trim();

                        JSONObject aiData = new JSONObject(aiReply);
                        String summaryText = aiData.getString("summary");
                        int pos = aiData.getInt("positive");
                        int neg = aiData.getInt("negative");
                        int neu = aiData.getInt("neutral");

                        runOnUiThread(() -> {
                            tvSummary.setText("📝 AI Summary: \n" + summaryText);
                            generateSentimentPieChart(pos, neg, neu, layout);
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> tvSummary.setText("❌ Failed to parse AI."));
                    }
                }
            });
        } catch (Exception e) {
            tvSummary.setText("❌ Error: " + e.getMessage());
        }
    }

    private void generateSentimentPieChart(int pos, int neg, int neu, LinearLayout layout) {
        if (pos == 0 && neg == 0 && neu == 0) return;

        PieChart pieChart = new PieChart(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 500);
        params.setMargins(0, 20, 0, 10);
        pieChart.setLayoutParams(params);
        pieChart.setDrawHoleEnabled(false);

        List<PieEntry> entries = new ArrayList<>();
        if (pos > 0) entries.add(new PieEntry(pos, "Positive"));
        if (neg > 0) entries.add(new PieEntry(neg, "Negative"));
        if (neu > 0) entries.add(new PieEntry(neu, "Neutral"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        int[] colors = {Color.parseColor("#4CAF50"), Color.parseColor("#F44336"), Color.parseColor("#9E9E9E")};
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        pieChart.setData(new PieData(dataSet));
        pieChart.getDescription().setEnabled(false);
        pieChart.invalidate();

        layout.addView(pieChart);
    }
}