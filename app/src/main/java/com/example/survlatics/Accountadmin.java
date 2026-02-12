package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private Button btnLogout;

    // ⭐ NEW
    private RecyclerView recyclerUsers;
    private UserAdapter userAdapter;
    private List<user> userList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_admin);

        // Firebase
        adminAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Secondary Auth (for creating users without logging admin out)
        FirebaseApp secondaryApp;
        try {
            secondaryApp = FirebaseApp.getInstance("Secondary");
        } catch (IllegalStateException e) {
            FirebaseOptions options = FirebaseApp.getInstance().getOptions();
            secondaryApp = FirebaseApp.initializeApp(this, options, "Secondary");
        }
        secondaryAuth = FirebaseAuth.getInstance(secondaryApp);

        // ---------------- UI ----------------
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnAddUser = findViewById(R.id.btnAddUser);
        btnLogout = findViewById(R.id.btnLogout);
        recyclerUsers = findViewById(R.id.recyclerUsers);

        // RecyclerView setup
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(userList, this);
        recyclerUsers.setAdapter(userAdapter);

        // Dropdown roles
        String[] roles = {"user", "admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                roles
        );
        spinnerRole.setAdapter(adapter);
        spinnerRole.setText("user", false);

        // ---------------- Bottom Navigation ----------------
        ImageButton btnHome = findViewById(R.id.nav_home);
        ImageButton btnMiddle = findViewById(R.id.nav_surveys);

        btnHome.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminActivity.class));
            finish();
        });

        btnMiddle.setOnClickListener(v -> {
            startActivity(new Intent(this, CompleteActivity.class));
            finish();
        });
        btnLogout.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        // ---------------- Admin Access Check ----------------
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
                        loadUsers(); // ⭐ LOAD USERS ONLY IF ADMIN
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


    // ⭐ LOAD USERS FROM FIRESTORE
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
                                    Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
