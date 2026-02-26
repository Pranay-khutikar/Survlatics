package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.button3);

        // 🔁 Auto-login if already authenticated
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // Disable the button and change text so the user knows it's loading
            btnLogin.setEnabled(false);
            btnLogin.setText("Logging in...");

            // Check their role in Firestore and send them to the correct screen
            redirectByRole(currentUser.getUid());

            return; // Stop the rest of the onCreate method from running so they skip the login process
        }

        // If not logged in, wait for them to click the login button
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {

        String email = etEmail.getText() != null
                ? etEmail.getText().toString().trim()
                : "";

        String password = etPassword.getText() != null
                ? etPassword.getText().toString().trim()
                : "";

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
        btnLogin.setText("Logging in...");

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    btnLogin.setEnabled(true);
                    btnLogin.setText("Login");

                    if (!task.isSuccessful()) {
                        Toast.makeText(
                                this,
                                "Invalid email or password",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    redirectByRole(user.getUid());
                });
    }

    private void redirectByRole(@NonNull String uid) {

        firestore.collection("Users") // keep EXACT case as your Firestore
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        Toast.makeText(
                                this,
                                "User record not found",
                                Toast.LENGTH_LONG
                        ).show();
                        firebaseAuth.signOut();
                        // Reset button if auto-login fails
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");
                        return;
                    }

                    String role = document.getString("role");

                    if (role == null) {
                        Toast.makeText(
                                this,
                                "User role missing",
                                Toast.LENGTH_SHORT
                        ).show();
                        firebaseAuth.signOut();
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");
                        return;
                    }

                    role = role.trim().toLowerCase();

                    Intent intent;

                    if (role.equals("admin")) {
                        intent = new Intent(this, AdminActivity.class);
                    } else if (role.equals("user")) {
                        intent = new Intent(this, HomeActivity.class);
                    } else {
                        Toast.makeText(
                                this,
                                "Invalid role configuration",
                                Toast.LENGTH_SHORT
                        ).show();
                        firebaseAuth.signOut();
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");
                        return;
                    }

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            this,
                            "Failed to load user data",
                            Toast.LENGTH_SHORT
                    ).show();
                    firebaseAuth.signOut();
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Login");
                });
    }
}