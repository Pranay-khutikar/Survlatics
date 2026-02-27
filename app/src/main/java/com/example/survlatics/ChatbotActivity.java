package com.example.survlatics;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    // Assume you created a ChatAdapter
    // private ChatAdapter adapter;
    private List<ChatMessage> messageList;
    private OkHttpClient client;

    private static final String SYSTEM_PROMPT = "You are an AI assistant for an app called Survlatics. Your job is to help users understand how to use the app. Keep answers short and friendly. To take a survey, users must go to the Home screen, find a pending survey, and click it to answer questions. To view completed surveys, they should use the bottom navigation menu.";

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
        // adapter = new ChatAdapter(messageList);
        // rvMessages.setAdapter(adapter);

        // Add a welcome message
        addMessage("Hello! I'm your Survlatics assistant. Need help taking a survey?", true);

        btnSend.setOnClickListener(v -> {
            String question = etMessage.getText().toString().trim();
            if (!question.isEmpty()) {
                addMessage(question, false);
                etMessage.setText("");
                callGeminiAPI(question);
            }
        });
    }

    private void addMessage(String message, boolean isBot) {
        runOnUiThread(() -> {
            messageList.add(new ChatMessage(message, isBot));
            // adapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.scrollToPosition(messageList.size() - 1);
        });
    }

    private void callGeminiAPI(String userText) {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        try {
            // Build the JSON payload for Gemini
            JSONObject jsonBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject partsObject = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject textObject = new JSONObject();

            // Prepend the system instructions so the bot knows its role
            String fullPrompt = SYSTEM_PROMPT + "\n\nUser asked: " + userText;
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
                    addMessage("Sorry, I'm having trouble connecting to the server.", true);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String responseBody = response.body().string();
                            JSONObject jsonObject = new JSONObject(responseBody);

                            // Parse Gemini's response structure
                            JSONArray candidates = jsonObject.getJSONArray("candidates");
                            JSONObject firstCandidate = candidates.getJSONObject(0);
                            JSONObject content = firstCandidate.getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            String botReply = parts.getJSONObject(0).getString("text");

                            addMessage(botReply, true);

                        } catch (Exception e) {
                            Log.e("Chatbot", "JSON Parsing error", e);
                            addMessage("I didn't understand the response from the server.", true);
                        }
                    } else {
                        addMessage("Error: " + response.code(), true);
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}