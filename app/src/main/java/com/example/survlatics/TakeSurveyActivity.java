package com.example.survlatics;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
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
import androidx.core.content.res.ResourcesCompat;

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

        btnSubmitSurvey.setOnClickListener(v -> {
            animateClick(v);
            submitAnswers();
        });
    }

    private void animateClick(View view) {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.95f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.95f, 1f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY);
        animator.setDuration(200);
        animator.start();
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
                        renderQuestion(qSnap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TakeSurveyActivity.this, "Error loading survey", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderQuestion(DataSnapshot qSnap) {
        String qId = qSnap.getKey();
        String type = qSnap.child("type").getValue(String.class);
        String text = qSnap.child("text").getValue(String.class);

        questionIds.add(qId);

        // 1. Stylized Question Title
        TextView tvQuestion = new TextView(this);
        tvQuestion.setText(text);
        tvQuestion.setTextSize(18f);
        tvQuestion.setTextColor(getResources().getColor(R.color.text_primary));
        tvQuestion.setTypeface(null, Typeface.BOLD);
        tvQuestion.setPadding(0, 40, 0, 16);
        questionsContainer.addView(tvQuestion);

        // 2. Themed Inputs
        if ("mcq".equals(type)) {
            RadioGroup radioGroup = new RadioGroup(this);
            radioGroup.setPadding(0, 10, 0, 20);

            DataSnapshot optionsSnap = qSnap.child("options");
            for (DataSnapshot optSnap : optionsSnap.getChildren()) {
                String optText = optSnap.child("text").getValue(String.class);

                RadioButton rb = new RadioButton(this);
                rb.setText(optText);
                rb.setTextColor(getResources().getColor(R.color.text_secondary));
                rb.setTextSize(16f);
                rb.setPadding(16, 16, 16, 16);

                // Set the dot color to match the glowing accent
                rb.setButtonTintList(ColorStateList.valueOf(getResources().getColor(R.color.accent_primary)));

                radioGroup.addView(rb);
            }
            questionsContainer.addView(radioGroup);
            answerViews.add(radioGroup);
        } else {
            EditText editText = new EditText(this);
            editText.setHint("Tap to type your answer...");
            editText.setHintTextColor(getResources().getColor(R.color.text_secondary));
            editText.setTextColor(getResources().getColor(R.color.text_primary));
            editText.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_chat_bot, getTheme()));
            editText.setPadding(40, 32, 40, 32);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 16, 0, 16);
            editText.setLayoutParams(params);

            questionsContainer.addView(editText);
            answerViews.add(editText);
        }
    }

    private void submitAnswers() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        Map<String, Object> responses = new HashMap<>();

        for (int i = 0; i < answerViews.size(); i++) {
            View view = answerViews.get(i);
            String qId = questionIds.get(i);
            String answerText = "";

            if (view instanceof RadioGroup) {
                RadioGroup rg = (RadioGroup) view;
                int selectedId = rg.getCheckedRadioButtonId();
                if (selectedId != -1) {
                    RadioButton rb = rg.findViewById(selectedId);
                    answerText = rb.getText().toString();
                }
            } else if (view instanceof EditText) {
                answerText = ((EditText) view).getText().toString().trim();
            }

            responses.put(qId, answerText);
        }

        db.child("responses").child(surveyId).child(userId).setValue(responses)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Response Recorded", Toast.LENGTH_SHORT).show();
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                });
    }
}