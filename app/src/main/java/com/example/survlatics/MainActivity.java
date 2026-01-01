package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        TextInputEditText etEmail = findViewById(R.id.etEmail);
        TextInputEditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.button3);

        btnLogin.setOnClickListener(v -> {

            String email = etEmail.getText() != null
                    ? etEmail.getText().toString().trim()
                    : "";

            String password = etPassword.getText() != null
                    ? etPassword.getText().toString().trim()
                    : "";

            // Validation
            if (email.isEmpty()) {
                Toast.makeText(MainActivity.this, "Email is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Password is required", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔐 Firebase Authentication (REAL LOGIN CHECK)
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            // Login success
                            Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish(); // prevent back to login

                        } else {

                            // Login failed
                            Toast.makeText(
                                    MainActivity.this,
                                    "Invalid email or password",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
        });
    }
}
