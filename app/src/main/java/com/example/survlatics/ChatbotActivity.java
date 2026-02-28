package com.example.survlatics;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatbotActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private Button btnSend;
    private ImageView btnBack;
    private ChatAdapter adapter;
    private List<ChatMessage> messageList;
    private OkHttpClient client;

    private String dynamicSystemPrompt = "You are a friendly, calm, and mindful AI assistant for an app called Survlatics. Your job is to help users understand how to use the app. Keep answers short, encouraging, and easy to read. ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        rvMessages = findViewById(R.id.rvChatMessages);
        etMessage = findViewById(R.id.etChatMessage);
        btnSend = findViewById(R.id.btnSendChat);
        btnBack = findViewById(R.id.btnBack);

        messageList = new ArrayList<>();
        client = new OkHttpClient();

        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(messageList);
        rvMessages.setAdapter(adapter);

        fetchUserRole();

        // Updated greeting to match the calmer vibe
        addMessage("Hello. I'm your Survlatics guide. How can I help you find clarity today?", true);

        // Back button logic with soft animation
        btnBack.setOnClickListener(v -> {
            animateClick(v);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        btnSend.setOnClickListener(v -> {
            animateClick(v);
            String question = etMessage.getText().toString().trim();
            if (!question.isEmpty()) {
                addMessage(question, false);
                etMessage.setText("");
                callGeminiAPI(question);
            }
        });
    }

    // Soft scale animation for interactions
    private void animateClick(View view) {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.90f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.90f, 1f);
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

    private void fetchUserRole() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.getString("role") != null) {
                        String role = document.getString("role").toLowerCase();

                        if (role.equals("admin")) {
                            dynamicSystemPrompt += "The person you are talking to is an ADMIN. Admins CAN create new surveys. Feel free to explain how to create surveys if they ask.";
                        } else {
                            dynamicSystemPrompt += "The person you are talking to is a STANDARD USER. Users CANNOT create surveys. If they ask how to create a survey, politely tell them only Admins can do that, and gently instruct them how to take an existing survey instead.";
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Chatbot", "Failed to fetch user role", e));
    }

    private void addMessage(String message, boolean isBot) {
        runOnUiThread(() -> {
            messageList.add(new ChatMessage(message, isBot));
            adapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.scrollToPosition(messageList.size() - 1);
        });
    }

    private void callGeminiAPI(String userText) {
        String rawKey = BuildConfig.GEMINI_API_KEY;
        String apiKey = rawKey.replace("\"", "").replace("\n", "").trim();

        if (apiKey.isEmpty()) {
            addMessage("System Error: Configuration missing. Please check your settings.", true);
            return;
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        try {
            JSONObject jsonBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject partsObject = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject textObject = new JSONObject();

            String fullPrompt = dynamicSystemPrompt + "\n\nUser asked: " + userText;
            textObject.put("text", fullPrompt);

            partsArray.put(textObject);
            partsObject.put("parts", partsArray);
            contentsArray.put(partsObject);
            jsonBody.put("contents", contentsArray);

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    addMessage("Network Error: Please check your connection and try again.", true);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonObject = new JSONObject(responseBody);
                            JSONArray candidates = jsonObject.getJSONArray("candidates");
                            JSONObject firstCandidate = candidates.getJSONObject(0);
                            JSONObject content = firstCandidate.getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            String botReply = parts.getJSONObject(0).getString("text");

                            addMessage(botReply, true);
                        } catch (Exception e) {
                            addMessage("I'm having trouble understanding that right now. Could you rephrase?", true);
                        }
                    } else {
                        Log.e("Chatbot", "API Error: " + responseBody);
                        addMessage("I encountered a server hiccup. Please try again in a moment.", true);
                    }
                }
            });

        } catch (Exception e) {
            addMessage("App Error: Something went wrong on my end.", true);
        }
    }
}