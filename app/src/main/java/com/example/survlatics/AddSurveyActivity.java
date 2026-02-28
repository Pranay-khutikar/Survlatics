package com.example.survlatics;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class AddSurveyActivity extends AppCompatActivity {

    private EditText etSurveyTitle;
    private LinearLayout questionContainer;
    private DatabaseReference db;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_survey);

        etSurveyTitle = findViewById(R.id.etSurveyTitle);
        questionContainer = findViewById(R.id.questionContainer);
        btnBack = findViewById(R.id.btnBack);
        db = FirebaseDatabase.getInstance().getReference();

        // Back button logic with soft animation
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                animateClick(v);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        findViewById(R.id.btnAddQuestion).setOnClickListener(v -> {
            animateClick(v);
            addQuestion();
        });

        findViewById(R.id.btnPublish).setOnClickListener(v -> {
            animateClick(v);
            publishSurvey();
        });
    }

    // --- Soft scale animation for interactions ---
    private void animateClick(View view) {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.95f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.95f, 1f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY);
        animator.setDuration(200);
        animator.start();
    }

    // Ensure system back button also fades smoothly
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void addQuestion() {
        // Because of animateLayoutChanges="true" in the XML, this view will smoothly expand inward
        View qView = getLayoutInflater().inflate(R.layout.item_question, questionContainer, false);

        AutoCompleteTextView typeSpinner = qView.findViewById(R.id.spinnerType);
        LinearLayout optionsContainer = qView.findViewById(R.id.optionsContainer);
        Button btnAddOption = qView.findViewById(R.id.btnAddOption);
        Button btnRemoveQuestion = qView.findViewById(R.id.btnRemoveQuestion);

        String[] types = {"text", "mcq"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, types);
        typeSpinner.setAdapter(adapter);

        typeSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            boolean isMcq = "mcq".equalsIgnoreCase(selected);

            optionsContainer.setVisibility(isMcq ? View.VISIBLE : View.GONE);
            btnAddOption.setVisibility(isMcq ? View.VISIBLE : View.GONE);

            if (!isMcq) optionsContainer.removeAllViews();
        });

        btnAddOption.setOnClickListener(v -> {
            animateClick(v);
            View optView = getLayoutInflater().inflate(R.layout.item_option, optionsContainer, false);
            optionsContainer.addView(optView);
        });

        btnRemoveQuestion.setOnClickListener(v -> {
            animateClick(v);
            questionContainer.removeView(qView);
        });

        questionContainer.addView(qView);
    }

    private void publishSurvey() {
        String title = etSurveyTitle.getText().toString().trim();
        if (title.isEmpty() || questionContainer.getChildCount() == 0) {
            Toast.makeText(this, "Please add a title and at least one question.", Toast.LENGTH_SHORT).show();
            return;
        }

        String surveyId = db.child("surveys").push().getKey();
        if (surveyId == null) return;

        Map<String, Object> survey = new HashMap<>();
        survey.put("title", title);
        survey.put("active", true);
        survey.put("createdBy", FirebaseAuth.getInstance().getUid());

        Map<String, Object> questions = new HashMap<>();

        for (int i = 0; i < questionContainer.getChildCount(); i++) {
            View qView = questionContainer.getChildAt(i);
            EditText etQ = qView.findViewById(R.id.etQuestion);
            AutoCompleteTextView typeS = qView.findViewById(R.id.spinnerType);
            LinearLayout optC = qView.findViewById(R.id.optionsContainer);

            Map<String, Object> qData = new HashMap<>();
            qData.put("text", etQ.getText().toString());
            qData.put("type", typeS.getText().toString());

            if ("mcq".equals(typeS.getText().toString())) {
                Map<String, Object> opts = new HashMap<>();
                for (int j = 0; j < optC.getChildCount(); j++) {
                    View oV = optC.getChildAt(j);
                    EditText etO = oV.findViewById(R.id.etOptionText);
                    CheckBox cbCorrect = oV.findViewById(R.id.checkOption);

                    Map<String, Object> optData = new HashMap<>();
                    optData.put("text", etO.getText().toString());
                    optData.put("isCorrect", cbCorrect.isChecked());

                    opts.put("option_" + (j + 1), optData);
                }
                qData.put("options", opts);
            }
            questions.put("question_" + (i + 1), qData);
        }

        survey.put("questions", questions);
        db.child("surveys").child(surveyId).setValue(survey)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Survey published successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                });
    }
}