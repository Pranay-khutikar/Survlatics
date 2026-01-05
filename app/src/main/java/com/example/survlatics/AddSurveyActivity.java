package com.example.survlatics;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_survey);

        etSurveyTitle = findViewById(R.id.etSurveyTitle);
        questionContainer = findViewById(R.id.questionContainer);

        db = FirebaseDatabase.getInstance().getReference();

        findViewById(R.id.btnAddQuestion).setOnClickListener(v -> addQuestion());
        findViewById(R.id.btnPublish).setOnClickListener(v -> publishSurvey());
    }

    private void addQuestion() {
        View qView = getLayoutInflater().inflate(R.layout.item_question, null);

        AutoCompleteTextView type = qView.findViewById(R.id.spinnerType);
        LinearLayout optionsContainer = qView.findViewById(R.id.optionsContainer);
        Button btnAddOption = qView.findViewById(R.id.btnAddOption);

        String[] types = {"text", "mcq"};
        type.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                types
        ));

        type.setOnItemClickListener((parent, view, position, id) -> {
            boolean isMcq = types[position].equals("mcq");
            optionsContainer.setVisibility(isMcq ? View.VISIBLE : View.GONE);
            btnAddOption.setVisibility(isMcq ? View.VISIBLE : View.GONE);
        });

        btnAddOption.setOnClickListener(v -> {
            View optView = getLayoutInflater().inflate(R.layout.item_option, null);
            optionsContainer.addView(optView);
        });

        qView.findViewById(R.id.btnRemoveQuestion)
                .setOnClickListener(v -> questionContainer.removeView(qView));

        questionContainer.addView(qView);
    }

    private void publishSurvey() {
        String title = etSurveyTitle.getText().toString().trim();

        if (title.isEmpty() || questionContainer.getChildCount() == 0) {
            Toast.makeText(this, "Add title and at least one question", Toast.LENGTH_SHORT).show();
            return;
        }

        String surveyId = db.child("surveys").push().getKey();

        Map<String, Object> survey = new HashMap<>();
        survey.put("title", title);
        survey.put("active", true);
        survey.put("createdBy", FirebaseAuth.getInstance().getUid());

        db.child("surveys").child(surveyId).setValue(survey);

        for (int i = 0; i < questionContainer.getChildCount(); i++) {
            View qView = questionContainer.getChildAt(i);

            EditText qText = qView.findViewById(R.id.etQuestion);
            AutoCompleteTextView type = qView.findViewById(R.id.spinnerType);
            LinearLayout optionsContainer = qView.findViewById(R.id.optionsContainer);

            String qId = db.child("surveys").child(surveyId)
                    .child("questions").push().getKey();

            db.child("surveys").child(surveyId)
                    .child("questions").child(qId)
                    .child("text").setValue(qText.getText().toString());

            db.child("surveys").child(surveyId)
                    .child("questions").child(qId)
                    .child("type").setValue(type.getText().toString());

            if ("mcq".equals(type.getText().toString())) {
                for (int j = 0; j < optionsContainer.getChildCount(); j++) {
                    View optView = optionsContainer.getChildAt(j);
                    EditText etOpt = optView.findViewById(R.id.etOptionText);

                    String optId = db.push().getKey();
                    db.child("surveys").child(surveyId)
                            .child("questions").child(qId)
                            .child("options").child(optId)
                            .child("text").setValue(etOpt.getText().toString());
                }
            }
        }

        Toast.makeText(this, "Survey published", Toast.LENGTH_SHORT).show();
        finish();
    }
}
