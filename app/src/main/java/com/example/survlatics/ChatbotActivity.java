package com.example.survlatics;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

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
    private ChatAdapter adapter;
    private List<ChatMessage> messageList;
    private OkHttpClient client;

    // We changed this from 'static final' so we can modify it based on the user's role!
    private String dynamicSystemPrompt = "You are an AI assistant for an app called Survlatics. Your job is to help users understand how to use the app. Keep answers short and friendly. ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        rvMessages = findViewById(R.id.rvChatMessages);
        etMessage = findViewById(R.id.etChatMessage);
        btnSend = findViewById(R.id.btnSendChat);

        messageList = new ArrayList<>();
        client = new OkHttpClient();

        // Setup RecyclerView
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(messageList);
        rvMessages.setAdapter(adapter);

        // Fetch the user's role from Firestore in the background
        fetchUserRole();

        addMessage("Hello! I'm your Survlatics assistant. Need help?", true);

        btnSend.setOnClickListener(v -> {
            String question = etMessage.getText().toString().trim();
            if (!question.isEmpty()) {
                addMessage(question, false);
                etMessage.setText("");
                callGeminiAPI(question);
            }
        });
    }

    // 🌟 NEW METHOD: Fetch the role and update the AI's instructions
    private void fetchUserRole() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.getString("role") != null) {
                        String role = document.getString("role").toLowerCase();

                        // Give the AI specific instructions based on who is currently logged in
                        if (role.equals("admin")) {
                            dynamicSystemPrompt += "The person you are talking to is an ADMIN. Admins CAN create new surveys. Feel free to explain how to create surveys if they ask.";
                        } else {
                            dynamicSystemPrompt += "The person you are talking to is a STANDARD USER. Users CANNOT create surveys. If they ask how to create a survey, politely tell them only Admins can do that, and instruct them how to take an existing survey instead.";
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
            addMessage("System Error: Gemini API Key is missing. Check local.properties.", true);
            return;
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        try {
            JSONObject jsonBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject partsObject = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject textObject = new JSONObject();

            // Append the dynamically generated prompt with the user's text
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
                    addMessage("Network Error: Make sure your emulator/phone has Wi-Fi enabled.", true);
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
                            addMessage("Error parsing AI response.", true);
                        }
                    } else {
                        Log.e("Chatbot", "API Error: " + responseBody);
                        if (response.code() == 400) {
                            addMessage("API Error 400: Your API key might be invalid.", true);
                        } else if (response.code() == 404) {
                            addMessage("API Error 404: Endpoint not found.", true);
                        } else {
                            addMessage("Server Error " + response.code() + ".", true);
                        }
                    }
                }
            });

        } catch (Exception e) {
            addMessage("App Error: Failed to build the JSON request.", true);
        }
    }
}