package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Accountadmin extends AppCompatActivity {

    private FirebaseAuth adminAuth;        // Logged-in admin
    private FirebaseAuth secondaryAuth;    // Used only for creating users
    private FirebaseFirestore firestore;

    private EditText etEmail, etPassword;
    private AutoCompleteTextView spinnerRole;
    private Button btnAddUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_admin);

        // Firebase init
        adminAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // ------------------ Secondary FirebaseAuth (CRITICAL FIX) ------------------
        FirebaseApp secondaryApp;
        try {
            secondaryApp = FirebaseApp.getInstance("Secondary");
        } catch (IllegalStateException e) {
            FirebaseOptions options = FirebaseApp.getInstance().getOptions();
            secondaryApp = FirebaseApp.initializeApp(this, options, "Secondary");
        }
        secondaryAuth = FirebaseAuth.getInstance(secondaryApp);

        // ------------------ Bottom Navigation ------------------
        ImageButton btnHome = findViewById(R.id.imageButton);
        ImageButton btnMiddle = findViewById(R.id.imageButton2);
        ImageButton btnAccount = findViewById(R.id.imageButton3);

        // ------------------ Admin UI ------------------
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnAddUser = findViewById(R.id.btnAddUser);

        // Dropdown setup
        String[] roles = {"user", "admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                roles
        );
        spinnerRole.setAdapter(adapter);
        spinnerRole.setText("user", false);

        // ------------------ Admin Access Check ------------------
        FirebaseUser currentUser = adminAuth.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        firestore.collection("Users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    if (!"admin".equalsIgnoreCase(role)) {
                        hideAdminUI();
                        Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> hideAdminUI());

        // ------------------ Actions ------------------
        btnAddUser.setOnClickListener(v -> addUser());

        btnHome.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminActivity.class));
            finish();
        });

        btnMiddle.setOnClickListener(v -> {
            startActivity(new Intent(this, CompleteActivity.class));
            finish();
        });

        // btnAccount → already on this screen
    }

    private void hideAdminUI() {
        etEmail.setVisibility(View.GONE);
        etPassword.setVisibility(View.GONE);
        spinnerRole.setVisibility(View.GONE);
        btnAddUser.setVisibility(View.GONE);
    }

    private void addUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String role = spinnerRole.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || role.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser newUser = result.getUser();
                    if (newUser == null) return;

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("email", email);
                    userData.put("role", role);

                    firestore.collection("Users")
                            .document(newUser.getUid())
                            .set(userData)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "User added successfully", Toast.LENGTH_SHORT).show();
                                etEmail.setText("");
                                etPassword.setText("");
                                spinnerRole.setText("user", false);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
