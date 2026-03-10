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
import com.github.mikephil.charting.formatter.PercentFormatter;
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
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_report);

        analysisContainer = findViewById(R.id.analysisContainer);
        tvReportTitle = findViewById(R.id.tvReportTitle);
        btnBack = findViewById(R.id.btnBack);
        db = FirebaseDatabase.getInstance().getReference();
        client = new OkHttpClient();

        // Safely grab the intent extra
        surveyId = getIntent().getStringExtra("SURVEY_ID");
        if (surveyId == null) surveyId = getIntent().getStringExtra("surveyId");
        if (surveyId == null) surveyId = getIntent().getStringExtra("id");

        setupNavigation();

        btnBack.setOnClickListener(v -> {
            animateClick(v);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        if (surveyId != null && !surveyId.isEmpty()) {
            fetchSurveyData();
        } else {
            Toast.makeText(this, "Error: Could not load Survey ID.", Toast.LENGTH_LONG).show();
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
                try {
                    if (!surveySnap.exists()) {
                        Toast.makeText(SurveyReportActivity.this, "Survey data not found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String title = surveySnap.child("title").getValue(String.class);
                    tvReportTitle.setText(title != null ? title : "Analysis");

                    Map<String, String> types = new HashMap<>();
                    Map<String, String> texts = new HashMap<>();

                    for (DataSnapshot qSnap : surveySnap.child("questions").getChildren()) {
                        types.put(qSnap.getKey(), qSnap.child("type").getValue(String.class));
                        texts.put(qSnap.getKey(), qSnap.child("text").getValue(String.class));
                    }
                    fetchResponses(types, texts);
                } catch (Exception e) {
                    Log.e("SurveyReport", "Error in fetchSurveyData", e);
                    Toast.makeText(SurveyReportActivity.this, "Error loading survey structure.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SurveyReportActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchResponses(Map<String, String> types, Map<String, String> texts) {
        db.child("responses").child(surveyId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot responsesSnap) {
                try {
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

                        List<String> answers = answersMap.containsKey(qId) ? answersMap.get(qId) : new ArrayList<>();

                        if (answers.isEmpty()) {
                            addNoDataMessage(cardLayout);
                        } else if ("mcq".equalsIgnoreCase(types.get(qId))) {
                            generateDonutChart(answers, cardLayout);
                        } else {
                            generateTextAnalysis(answers, cardLayout);
                        }
                    }
                } catch (Exception e) {
                    Log.e("SurveyReport", "Crash in fetchResponses", e);
                    Toast.makeText(SurveyReportActivity.this, "Crash: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        for (String ans : answers) {
            if (ans != null) {
                int count = freqMap.containsKey(ans) ? freqMap.get(ans) : 0;
                freqMap.put(ans, count + 1);
            }
        }

        PieChart chart = new PieChart(this);

        int heightPx = (int) (280 * getResources().getDisplayMetrics().density); // Slightly taller for labels
        chart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx));

        // --- NEW: Enable percentage display ---
        chart.setUsePercentValues(true);

        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setHoleRadius(50f);
        chart.setTransparentCircleRadius(55f);
        chart.setCenterText("Data Distribution");
        chart.setCenterTextColor(getResources().getColor(R.color.text_secondary));
        chart.setEntryLabelColor(Color.WHITE); // Make inner labels white for readability

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> e : freqMap.entrySet()) {
            entries.add(new PieEntry(e.getValue(), e.getKey()));
        }

        if (entries.isEmpty()) return;

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(new int[]{
                Color.parseColor("#38BDF8"),
                Color.parseColor("#818CF8"),
                Color.parseColor("#F472B6"),
                Color.parseColor("#34D399"),
                Color.parseColor("#FBBF24")
        });

        // --- NEW: Apply Percentage Formatter to the data ---
        PieData data = new PieData(set);
        data.setValueFormatter(new PercentFormatter(chart));
        data.setValueTextSize(14f);
        data.setValueTextColor(Color.WHITE);

        chart.setData(data);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(true);
        chart.getLegend().setTextColor(getResources().getColor(R.color.text_secondary));
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        chart.getLegend().setWordWrapEnabled(true);

        chart.invalidate();
        layout.addView(chart);
    }

    private void generateTextAnalysis(List<String> answers, LinearLayout layout) {
        Map<String, Integer> freqMap = new HashMap<>();
        for (String ans : answers) {
            if (ans != null) {
                String clean = ans.toLowerCase().trim();
                int count = freqMap.containsKey(clean) ? freqMap.get(clean) : 0;
                freqMap.put(clean, count + 1);
            }
        }

        String topTheme = "None";
        int maxCount = -1;
        for (Map.Entry<String, Integer> entry : freqMap.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                topTheme = entry.getKey();
            }
        }

        TextView tvTop = new TextView(this);
        tvTop.setText("Common Theme: " + topTheme);
        tvTop.setTextSize(16f);
        tvTop.setTextColor(Color.parseColor("#38BDF8"));
        tvTop.setPadding(0, 0, 0, 16);
        layout.addView(tvTop);

        TextView tvAi = new TextView(this);
        tvAi.setText("Synthesizing AI insights and sentiment...");
        tvAi.setTextColor(getResources().getColor(R.color.text_secondary));
        layout.addView(tvAi);

        fetchGeminiAnalysis(answers, tvAi, layout);
    }

    private void fetchGeminiAnalysis(List<String> answers, TextView tvAi, LinearLayout layout) {
        String prompt = "Analyze these survey responses: " + answers.toString() +
                "\nProvide the output strictly as a JSON object with this exact format: " +
                "{\"summary\": \"A short 2-3 sentence summary of the overall sentiment and main points\", " +
                "\"positive\": <number_of_positive_responses>, " +
                "\"negative\": <number_of_negative_responses>, " +
                "\"neutral\": <number_of_neutral_responses>}. " +
                "Do not include any markdown formatting like ```json.";

        JSONObject jsonBody = new JSONObject();
        try {
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", prompt);
            parts.put(part);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            jsonBody.put("contents", contents);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        String rawKey = BuildConfig.GEMINI_API_KEY;
        String apiKey = rawKey.replace("\"", "").replace("\n", "").trim();

        String url = "[https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=](https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=)" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (isFinishing() || isDestroyed()) return;
                runOnUiThread(() -> tvAi.setText("Failed to generate insights. Check internet connection."));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (isFinishing() || isDestroyed()) return;
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        String aiResponseText = jsonObject.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        aiResponseText = aiResponseText.replace("```json", "").replace("```", "").trim();

                        JSONObject resultJson = new JSONObject(aiResponseText);
                        String summary = resultJson.has("summary") ? resultJson.getString("summary") : "Analysis generated.";
                        int pos = resultJson.has("positive") ? resultJson.getInt("positive") : 0;
                        int neg = resultJson.has("negative") ? resultJson.getInt("negative") : 0;
                        int neu = resultJson.has("neutral") ? resultJson.getInt("neutral") : 0;

                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            tvAi.setText("AI Insight:\n" + summary);
                            generateSentimentChart(pos, neg, neu, layout);
                        });

                    } catch (Exception e) {
                        Log.e("SurveyReport", "Parsing Error", e);
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) tvAi.setText("Error parsing AI JSON insights.");
                        });
                    }
                } else {
                    Log.e("SurveyReport", "Google API Error " + response.code() + ": " + responseBody);
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) tvAi.setText("Error " + response.code() + " fetching insights.");
                    });
                }
            }
        });
    }

    private void generateSentimentChart(int pos, int neg, int neu, LinearLayout layout) {
        try {
            if (pos <= 0 && neg <= 0 && neu <= 0) return;

            PieChart chart = new PieChart(this);

            int heightPx = (int) (280 * getResources().getDisplayMetrics().density);
            int topMarginPx = (int) (16 * getResources().getDisplayMetrics().density);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
            params.topMargin = topMarginPx;
            chart.setLayoutParams(params);

            // --- NEW: Enable percentage display for Sentiment Chart ---
            chart.setUsePercentValues(true);

            chart.setDrawHoleEnabled(true);
            chart.setHoleColor(Color.TRANSPARENT);
            chart.setHoleRadius(50f);
            chart.setTransparentCircleRadius(55f);
            chart.setCenterText("AI Sentiment");
            chart.setCenterTextColor(getResources().getColor(R.color.text_secondary));
            chart.setEntryLabelColor(Color.WHITE);

            List<PieEntry> entries = new ArrayList<>();
            List<Integer> colorsList = new ArrayList<>();

            if (pos > 0) {
                entries.add(new PieEntry(pos, "Positive"));
                colorsList.add(Color.parseColor("#34D399"));
            }
            if (neg > 0) {
                entries.add(new PieEntry(neg, "Negative"));
                colorsList.add(Color.parseColor("#F87171"));
            }
            if (neu > 0) {
                entries.add(new PieEntry(neu, "Neutral"));
                colorsList.add(Color.parseColor("#94A3B8"));
            }

            if (entries.isEmpty()) return;

            int[] colorsArray = new int[colorsList.size()];
            for (int i = 0; i < colorsList.size(); i++) {
                colorsArray[i] = colorsList.get(i);
            }

            PieDataSet set = new PieDataSet(entries, "");
            set.setColors(colorsArray);

            // --- NEW: Apply Percentage Formatter to the Sentiment data ---
            PieData data = new PieData(set);
            data.setValueFormatter(new PercentFormatter(chart));
            data.setValueTextSize(14f);
            data.setValueTextColor(Color.WHITE);

            chart.setData(data);
            chart.getDescription().setEnabled(false);
            chart.getLegend().setEnabled(true);
            chart.getLegend().setTextColor(getResources().getColor(R.color.text_secondary));
            chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            chart.getLegend().setWordWrapEnabled(true);

            chart.invalidate();
            layout.addView(chart);
        } catch (Exception e) {
            Log.e("SurveyReport", "Error generating sentiment chart", e);
        }
    }
}