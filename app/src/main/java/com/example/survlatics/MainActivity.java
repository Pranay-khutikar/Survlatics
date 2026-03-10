package com.example.survlatics;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private LinearLayout loginContainer, otpContainer;
    private TextInputEditText etEmail, etPassword, etOtp;
    private Button btnLogin, btnVerifyOtp;
    private TextView tvOtpMessage;

    private String generatedOtp = "";
    private String currentUid = "";

    // --- EMAILJS CONFIGURATION ---
    // Double-check these in your EmailJS Dashboard!
    private static final String EMAILJS_SERVICE_ID = "service_survlatics";
    private static final String EMAILJS_TEMPLATE_ID = "template_survlatics";
    private static final String EMAILJS_PUBLIC_KEY = "jb4TbHmcQrX6ClNDM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        loginContainer = findViewById(R.id.loginContainer);
        otpContainer = findViewById(R.id.otpContainer);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etOtp = findViewById(R.id.etOtp);
        btnLogin = findViewById(R.id.button3);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        tvOtpMessage = findViewById(R.id.tvOtpMessage);

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            currentUid = currentUser.getUid();
            initiateOtpFlow(currentUser.getEmail());
        }

        btnLogin.setOnClickListener(v -> {
            animateClick(v);
            attemptLogin();
        });

        btnVerifyOtp.setOnClickListener(v -> {
            animateClick(v);
            verifyOtp();
        });
    }

    private void animateClick(View view) {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.95f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.95f, 1f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY);
        animator.setDuration(200);
        animator.start();
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Signing in...");

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Sign In");

                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            currentUid = user.getUid();
                            initiateOtpFlow(email);
                        }
                    } else {
                        Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initiateOtpFlow(String email) {
        generatedOtp = String.format("%06d", new Random().nextInt(1000000));

        loginContainer.setVisibility(View.GONE);
        otpContainer.setVisibility(View.VISIBLE);
        tvOtpMessage.setText("We sent a 6-digit code to\n" + email);

        sendEmailOTP(email, generatedOtp);
    }

    private void verifyOtp() {
        String enteredOtp = etOtp.getText() != null ? etOtp.getText().toString().trim() : "";

        if (enteredOtp.equals(generatedOtp)) {
            btnVerifyOtp.setText("Loading...");
            btnVerifyOtp.setEnabled(false);
            redirectByRole(currentUid);
        } else {
            Toast.makeText(this, "Invalid OTP Code", Toast.LENGTH_SHORT).show();
            etOtp.setText("");
        }
    }

    private void sendEmailOTP(String recipientEmail, String otpCode) {
        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject json = new JSONObject();
            json.put("service_id", EMAILJS_SERVICE_ID);
            json.put("template_id", EMAILJS_TEMPLATE_ID);
            json.put("user_id", EMAILJS_PUBLIC_KEY); // Must be "user_id" for the API

            JSONObject templateParams = new JSONObject();
            templateParams.put("to_email", recipientEmail); // Ensure dashboard is {{to_email}}
            templateParams.put("otp_code", otpCode);        // Ensure dashboard is {{otp_code}}
            json.put("template_params", templateParams);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url("https://api.emailjs.com/api/v1.0/email/send")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Network Error", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        final String errorMsg = response.body() != null ? response.body().string() : "Unknown Error";
                        Log.e("EmailJS", "Failed: " + errorMsg);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "API Error: " + errorMsg, Toast.LENGTH_LONG).show());
                    } else {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "OTP Sent Successfully!", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void redirectByRole(@NonNull String uid) {
        firestore.collection("Users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String role = document.getString("role");
                        Intent intent;
                        if ("admin".equalsIgnoreCase(role)) {
                            intent = new Intent(this, AdminActivity.class);
                        } else {
                            intent = new Intent(this, HomeActivity.class);
                        }
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void resetUI() {
        loginContainer.setVisibility(View.VISIBLE);
        otpContainer.setVisibility(View.GONE);
        etOtp.setText("");
    }
}