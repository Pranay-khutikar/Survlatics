package com.example.survlatics;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.os.Bundle;
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

    // --- EMAILJS CONFIGURATION (Replace with your actual keys from emailjs.com) ---
    private static final String EMAILJS_SERVICE_ID = "YOUR_SERVICE_ID";
    private static final String EMAILJS_TEMPLATE_ID = "YOUR_TEMPLATE_ID";
    private static final String EMAILJS_PUBLIC_KEY = "YOUR_PUBLIC_KEY";

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

        // 🔁 Handle existing user sessions
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

    // --- Soft Scale Animation for Interactions ---
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

        if (email.isEmpty()) {
            etEmail.setError("Email required");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password required");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Signing in...");

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Sign In");

                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        currentUid = user.getUid();
                        initiateOtpFlow(email);
                    }
                });
    }

    private void initiateOtpFlow(String email) {
        // 1. Generate 6 digit OTP
        generatedOtp = String.format("%06d", new Random().nextInt(999999));

        // 2. Transition UI
        loginContainer.setVisibility(View.GONE);
        otpContainer.setVisibility(View.VISIBLE);
        tvOtpMessage.setText("We sent a 6-digit code to\n" + email);

        // 3. Send Email via API
        sendEmailOTP(email, generatedOtp);

        // Uncomment this for fast local testing without checking email:
        // Toast.makeText(this, "DEBUG OTP: " + generatedOtp, Toast.LENGTH_LONG).show();
    }

    private void verifyOtp() {
        String enteredOtp = etOtp.getText() != null ? etOtp.getText().toString().trim() : "";

        if (enteredOtp.isEmpty() || enteredOtp.length() < 6) {
            etOtp.setError("Enter 6-digit OTP");
            return;
        }

        if (enteredOtp.equals(generatedOtp)) {
            // Success! Load user role
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
            json.put("user_id", EMAILJS_PUBLIC_KEY);

            JSONObject templateParams = new JSONObject();
            templateParams.put("to_email", recipientEmail);
            templateParams.put("otp_code", otpCode);
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
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Network error sending OTP", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    if(!response.isSuccessful()){
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Email API Error. Check API Keys.", Toast.LENGTH_LONG).show());
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
                    if (!document.exists()) {
                        Toast.makeText(this, "User record not found", Toast.LENGTH_LONG).show();
                        firebaseAuth.signOut();
                        resetUI();
                        return;
                    }

                    String role = document.getString("role");
                    if (role == null) {
                        Toast.makeText(this, "User role missing", Toast.LENGTH_SHORT).show();
                        firebaseAuth.signOut();
                        resetUI();
                        return;
                    }

                    Intent intent;
                    if (role.trim().equalsIgnoreCase("admin")) {
                        intent = new Intent(this, AdminActivity.class);
                    } else if (role.trim().equalsIgnoreCase("user")) {
                        intent = new Intent(this, HomeActivity.class);
                    } else {
                        Toast.makeText(this, "Invalid role configuration", Toast.LENGTH_SHORT).show();
                        firebaseAuth.signOut();
                        resetUI();
                        return;
                    }

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    firebaseAuth.signOut();
                    resetUI();
                });
    }

    private void resetUI() {
        loginContainer.setVisibility(View.VISIBLE);
        otpContainer.setVisibility(View.GONE);
        btnVerifyOtp.setEnabled(true);
        btnVerifyOtp.setText("Verify & Proceed");
        etOtp.setText("");
        etPassword.setText("");
    }
}