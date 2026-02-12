package com.example.survlatics;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
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
        View qView = getLayoutInflater().inflate(R.layout.item_question, questionContainer, false);

        AutoCompleteTextView typeSpinner = qView.findViewById(R.id.spinnerType);
        LinearLayout optionsContainer = qView.findViewById(R.id.optionsContainer);
        Button btnAddOption = qView.findViewById(R.id.btnAddOption);
        Button btnRemoveQuestion = qView.findViewById(R.id.btnRemoveQuestion);

        String[] types = {"text", "mcq"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, types);
        typeSpinner.setAdapter(adapter);

        // Improved Click Listener for the Dropdown
        typeSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            boolean isMcq = "mcq".equalsIgnoreCase(selected);

            optionsContainer.setVisibility(isMcq ? View.VISIBLE : View.GONE);
            btnAddOption.setVisibility(isMcq ? View.VISIBLE : View.GONE);

            if (!isMcq) optionsContainer.removeAllViews();
        });

        btnAddOption.setOnClickListener(v -> {
            View optView = getLayoutInflater().inflate(R.layout.item_option, optionsContainer, false);
            optionsContainer.addView(optView);
        });

        btnRemoveQuestion.setOnClickListener(v -> questionContainer.removeView(qView));
        questionContainer.addView(qView);
    }

    private void publishSurvey() {
        String title = etSurveyTitle.getText().toString().trim();
        if (title.isEmpty() || questionContainer.getChildCount() == 0) {
            Toast.makeText(this, "Complete the survey details", Toast.LENGTH_SHORT).show();
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
                    optData.put("isCorrect", cbCorrect.isChecked()); // Saves if this is the right answer

                    opts.put("option_" + (j + 1), optData);
                }
                qData.put("options", opts);
            }
            questions.put("question_" + (i + 1), qData);
        }

        survey.put("questions", questions);
        db.child("surveys").child(surveyId).setValue(survey)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Published!", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}