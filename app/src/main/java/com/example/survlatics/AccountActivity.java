package com.example.survlatics;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AccountActivity extends AppCompatActivity {

    private ImageView ivUserProfile, ivEditName;
    private TextView tvUserName, tvUserEmail;

    // Launcher to handle picking an image from the gallery
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account);

        // 1. Find UI Elements
        ivUserProfile = findViewById(R.id.ivUserProfile);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        ivEditName = findViewById(R.id.ivEditName);
        Button btnLogout = findViewById(R.id.btnLogout2);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // 2. Fetch User Data from Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvUserEmail.setText(user.getEmail());
            // Show display name if it exists, otherwise show default text
            tvUserName.setText(user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                    ? user.getDisplayName() : "Survey User");
        } else {
            tvUserName.setText("Guest");
            tvUserEmail.setText("Not logged in");
        }

        // 3. Load Profile Image (Check Local Storage First)
        File localProfileImage = new File(getFilesDir(), "profile_image.jpg");
        if (localProfileImage.exists()) {
            // If we have a locally saved image, load it
            Glide.with(this).load(localProfileImage).circleCrop().into(ivUserProfile);
        } else if (user != null && user.getPhotoUrl() != null) {
            // Otherwise, if they have an old Firebase image, load that
            Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(ivUserProfile);
        }

        // 4. Edit Name Logic (Show Popup Dialog)
        ivEditName.setOnClickListener(v -> showEditNameDialog());

        // 5. Setup Image Picker Logic
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();

                        // Show the selected image immediately using Glide
                        Glide.with(this)
                                .load(selectedImageUri)
                                .circleCrop()
                                .into(ivUserProfile);

                        // Save the image LOCALLY to the device
                        saveImageToInternalStorage(selectedImageUri);
                    }
                }
        );

        // 6. Click listener on Profile Image to open Gallery
        ivUserProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // 7. Logout Button Logic
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut(); // Sign out of Firebase

            Intent intent = new Intent(AccountActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            finish();
        });

        // 8. Bottom Navigation Logic
        bottomNavigationView.setSelectedItemId(R.id.nav_account);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_surveys) {
                startActivity(new Intent(this, CompleteActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_account) {
                return true;
            }
            return false;
        });
    }

    // --- NEW METHOD: Save Image Locally to Phone Storage ---
    private void saveImageToInternalStorage(Uri uri) {
        try {
            // Open the image file the user selected
            InputStream inputStream = getContentResolver().openInputStream(uri);

            // Create a new file in the app's private internal storage
            File file = new File(getFilesDir(), "profile_image.jpg");
            FileOutputStream outputStream = new FileOutputStream(file);

            // Copy the image data into our new local file
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // Close the streams
            outputStream.close();
            inputStream.close();

            Toast.makeText(this, "Profile image saved locally!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image locally.", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to show a dialog to edit the user's name
    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Name");

        // Set up the input field inside the dialog
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        // Put the current name in the box, unless it says "Survey User"
        String currentName = tvUserName.getText().toString();
        if (!currentName.equals("Survey User")) {
            input.setText(currentName);
        }

        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateNameInFirebase(newName);
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Method to save the new name to Firebase Authentication
    private void updateNameInFirebase(String newName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Update the screen with the new name
                            tvUserName.setText(newName);
                            Toast.makeText(AccountActivity.this, "Name updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(AccountActivity.this, "Failed to update name", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}