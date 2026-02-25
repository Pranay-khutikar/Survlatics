package com.example.survlatics;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TakeSurveyActivity extends AppCompatActivity {

    private TextView tvTakeSurveyTitle;
    private LinearLayout questionsContainer;
    private Button btnSubmitSurvey;
    private DatabaseReference db;
    private String surveyId;

    // To track generated views to extract answers later
    private List<View> answerViews = new ArrayList<>();
    private List<String> questionIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_survey);

        tvTakeSurveyTitle = findViewById(R.id.tvTakeSurveyTitle);
        questionsContainer = findViewById(R.id.questionsContainer);
        btnSubmitSurvey = findViewById(R.id.btnSubmitSurvey);

        surveyId = getIntent().getStringExtra("SURVEY_ID");
        db = FirebaseDatabase.getInstance().getReference();

        if (surveyId != null) {
            loadSurveyData();
        }

        btnSubmitSurvey.setOnClickListener(v -> submitAnswers());
    }

    private void loadSurveyData() {
        db.child("surveys").child(surveyId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String title = snapshot.child("title").getValue(String.class);
                    tvTakeSurveyTitle.setText(title);

                    DataSnapshot questionsSnap = snapshot.child("questions");
                    for (DataSnapshot qSnap : questionsSnap.getChildren()) {
                        String qId = qSnap.getKey();
                        String type = qSnap.child("type").getValue(String.class);
                        String text = qSnap.child("text").getValue(String.class);

                        questionIds.add(qId);

                        // Render Question Text
                        TextView tvQuestion = new TextView(TakeSurveyActivity.this);
                        tvQuestion.setText(text);
                        tvQuestion.setTextSize(18f);
                        tvQuestion.setPadding(0, 20, 0, 10);
                        questionsContainer.addView(tvQuestion);

                        // Render Inputs
                        if ("mcq".equals(type)) {
                            RadioGroup radioGroup = new RadioGroup(TakeSurveyActivity.this);
                            DataSnapshot optionsSnap = qSnap.child("options");
                            for (DataSnapshot optSnap : optionsSnap.getChildren()) {
                                String optText = optSnap.child("text").getValue(String.class);
                                RadioButton rb = new RadioButton(TakeSurveyActivity.this);
                                rb.setText(optText);
                                radioGroup.addView(rb);
                            }
                            questionsContainer.addView(radioGroup);
                            answerViews.add(radioGroup);
                        } else {
                            EditText editText = new EditText(TakeSurveyActivity.this);
                            editText.setHint("Type your answer here");
                            questionsContainer.addView(editText);
                            answerViews.add(editText);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TakeSurveyActivity.this, "Error loading survey", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitAnswers() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> responses = new HashMap<>();

        for (int i = 0; i < answerViews.size(); i++) {
            View view = answerViews.get(i);
            String qId = questionIds.get(i);
            String answerText = "";

            if (view instanceof RadioGroup) {
                RadioGroup rg = (RadioGroup) view;
                int selectedId = rg.getCheckedRadioButtonId();
                if (selectedId != -1) {
                    RadioButton rb = findViewById(selectedId);
                    answerText = rb.getText().toString();
                }
            } else if (view instanceof EditText) {
                answerText = ((EditText) view).getText().toString().trim();
            }

            responses.put(qId, answerText);
        }

        // Save to Database under responses -> surveyId -> userId
        db.child("responses").child(surveyId).child(userId).setValue(responses)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(TakeSurveyActivity.this, "Survey Submitted!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(TakeSurveyActivity.this, "Failed to submit", Toast.LENGTH_SHORT).show());
    }
}