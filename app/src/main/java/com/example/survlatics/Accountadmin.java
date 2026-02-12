package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;

public class Accountadmin extends AppCompatActivity {

    private FirebaseAuth adminAuth;
    private FirebaseAuth secondaryAuth;
    private FirebaseFirestore firestore;

    private EditText etEmail, etPassword;
    private AutoCompleteTextView spinnerRole;
    private Button btnAddUser;
    private Button btnLogout; // Use this variable for the logout logic

    private RecyclerView recyclerUsers;
    private UserAdapter userAdapter;
    private List<user> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_admin);

        // --- Firebase Setup ---
        adminAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Secondary Auth Setup
        FirebaseApp secondaryApp;
        try {
            secondaryApp = FirebaseApp.getInstance("Secondary");
        } catch (IllegalStateException e) {
            FirebaseOptions options = FirebaseApp.getInstance().getOptions();
            secondaryApp = FirebaseApp.initializeApp(this, options, "Secondary");
        }
        secondaryAuth = FirebaseAuth.getInstance(secondaryApp);

        // --- UI Initialization ---
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnAddUser = findViewById(R.id.btnAddUser);

        // Initializing the logout button (using the ID from your layout)
        btnLogout = findViewById(R.id.btnLogout);
        recyclerUsers = findViewById(R.id.recyclerUsers);

        // RecyclerView setup
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(userList, this);
        recyclerUsers.setAdapter(userAdapter);

        // Dropdown roles
        String[] roles = {"user", "admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, roles);
        spinnerRole.setAdapter(adapter);
        spinnerRole.setText("user", false);

        // --- Bottom Navigation ---
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Highlight the 'Account' icon because we are on the Account Admin page
        bottomNavigationView.setSelectedItemId(R.id.nav_account);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // Navigate back to the Admin dashboard
                startActivity(new Intent(this, AdminActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_surveys) {
                // Future: Manage published surveys
                return true;
            } else if (id == R.id.nav_account) {
                // Already here
                return true;
            }
            return false;
        });

        // --- Logout Button Action ---
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                adminAuth.signOut();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // --- Admin Access Check ---
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
                    } else {
                        loadUsers();
                    }
                })
                .addOnFailureListener(e -> hideAdminUI());

        btnAddUser.setOnClickListener(v -> addUser());
    }

    private void hideAdminUI() {
        etEmail.setVisibility(View.GONE);
        etPassword.setVisibility(View.GONE);
        spinnerRole.setVisibility(View.GONE);
        btnAddUser.setVisibility(View.GONE);
        recyclerUsers.setVisibility(View.GONE);
    }

    private void loadUsers() {
        firestore.collection("Users")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    userList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String email = doc.getString("email");
                        String role = doc.getString("role");
                        String uid = doc.getId();
                        userList.add(new user(email, role, uid));
                    }
                    userAdapter.notifyDataSetChanged();
                });
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
                                    Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}