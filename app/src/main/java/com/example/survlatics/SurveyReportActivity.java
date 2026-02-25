package com.example.survlatics;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
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

        analysisContainer = findViewById(R.id.analysisContainer);
        tvReportTitle = findViewById(R.id.tvReportTitle);
        surveyId = getIntent().getStringExtra("SURVEY_ID");
        db = FirebaseDatabase.getInstance().getReference();

        if (surveyId != null) {
            fetchSurveyData();
        } else {
            Toast.makeText(this, "Error: No Survey ID provided", Toast.LENGTH_SHORT).show();
            finish();
        }
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

                    addQuestionTitle(qText);

                    if (answers.isEmpty()) {
                        addNoDataMessage();
                        continue;
                    }

                    if ("mcq".equalsIgnoreCase(type)) {
                        generateDonutChart(answers);
                    } else if ("text".equalsIgnoreCase(type)) {
                        generateTextAnalysis(answers);
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
        analysisContainer.addView(tvReportTitle);
    }

    private void addQuestionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(18f);
        tv.setTextColor(Color.BLACK);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, 40, 0, 16);
        analysisContainer.addView(tv);
    }

    private void addNoDataMessage() {
        TextView tv = new TextView(this);
        tv.setText("No responses yet.");
        tv.setTextColor(Color.GRAY);
        tv.setPadding(0, 0, 0, 20);
        analysisContainer.addView(tv);
    }

    private void generateDonutChart(List<String> answers) {
        Map<String, Integer> freqMap = new HashMap<>();
        for (String ans : answers) {
            freqMap.put(ans, freqMap.getOrDefault(ans, 0) + 1);
        }

        PieChart pieChart = new PieChart(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 700);
        params.setMargins(0, 0, 0, 40);
        pieChart.setLayoutParams(params);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setCenterText("MCQ\nResults");
        pieChart.setCenterTextSize(14f);

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : freqMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);

        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        pieChart.invalidate();
        analysisContainer.addView(pieChart);
    }

    private void generateTextAnalysis(List<String> answers) {
        Map<String, Integer> freqMap = new HashMap<>();
        for (String ans : answers) {
            String lower = ans.toLowerCase().trim();
            freqMap.put(lower, freqMap.getOrDefault(lower, 0) + 1);
        }
        String mostRepeated = Collections.max(freqMap.entrySet(), Map.Entry.comparingByValue()).getKey();

        TextView tvMostRepeated = new TextView(this);
        tvMostRepeated.setText("📌 Most Repeated Answer: \n\"" + mostRepeated + "\"");
        tvMostRepeated.setTextSize(16f);
        tvMostRepeated.setTextColor(Color.parseColor("#00796B"));
        tvMostRepeated.setPadding(0, 10, 0, 20);
        analysisContainer.addView(tvMostRepeated);

        TextView tvSummary = new TextView(this);
        tvSummary.setText("🤖 AI is analyzing responses...");
        tvSummary.setTextSize(14f);
        tvSummary.setTextColor(Color.DKGRAY);
        tvSummary.setPadding(0, 20, 0, 40);
        analysisContainer.addView(tvSummary);

        fetchGeminiAnalysis(answers, tvSummary);
    }

    private void fetchGeminiAnalysis(List<String> answers, TextView tvSummary) {
        String prompt = "Analyze these survey responses: " + answers.toString() + ". " +
                "Return ONLY a valid JSON object. Required keys: " +
                "'summary' (2-sentence feedback summary), " +
                "'positive' (integer), 'negative' (integer), 'neutral' (integer). " +
                "Do not use markdown backticks.";

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        // Fixed URL to v1 stable endpoint to prevent 404
// Change "gemini-1.5-flash" to "gemini-2.5-flash" or "gemini-3-flash"
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
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> tvSummary.setText("❌ AI Analysis failed: " + e.getMessage()));
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
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        aiReply = aiReply.replace("```json", "").replace("```", "").trim();

                        JSONObject aiData = new JSONObject(aiReply);
                        String summaryText = aiData.getString("summary");
                        int pos = aiData.getInt("positive");
                        int neg = aiData.getInt("negative");
                        int neu = aiData.getInt("neutral");

                        runOnUiThread(() -> {
                            tvSummary.setText("📝 AI Summary: \n" + summaryText);
                            generateSentimentPieChart(pos, neg, neu);
                        });

                    } catch (Exception e) {
                        Log.e("JSON_ERROR", "Parsing failed: " + e.getMessage());
                        runOnUiThread(() -> tvSummary.setText("❌ Failed to parse AI response."));
                    }
                }
            });
        } catch (Exception e) {
            tvSummary.setText("❌ Error building request: " + e.getMessage());
        }
    }

    private void generateSentimentPieChart(int pos, int neg, int neu) {
        if (pos == 0 && neg == 0 && neu == 0) return;

        PieChart pieChart = new PieChart(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 600);
        params.setMargins(0, 10, 0, 40);
        pieChart.setLayoutParams(params);

        pieChart.setDrawHoleEnabled(false);

        List<PieEntry> entries = new ArrayList<>();
        if (pos > 0) entries.add(new PieEntry(pos, "Positive"));
        if (neg > 0) entries.add(new PieEntry(neg, "Negative"));
        if (neu > 0) entries.add(new PieEntry(neu, "Neutral"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        int[] colors = {Color.parseColor("#4CAF50"), Color.parseColor("#F44336"), Color.parseColor("#9E9E9E")};
        dataSet.setColors(colors);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);

        pieChart.setData(new PieData(dataSet));
        pieChart.getDescription().setEnabled(false);
        pieChart.invalidate();

        analysisContainer.addView(pieChart);
    }
}