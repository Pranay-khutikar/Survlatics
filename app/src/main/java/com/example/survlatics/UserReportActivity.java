package com.example.survlatics;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserReportActivity extends AppCompatActivity {

    private TextInputEditText etReportMessage;
    private MaterialButton btnSubmitReport, btnAttachImage;
    private ImageView ivImagePreview;
    private BottomNavigationView bottomNavigationView;

    private Uri imageUri = null;

    // YOUR IMGBB API KEY
    private final String IMGBB_API_KEY = "50b4119135b90f9953e85404da81db13";

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    ivImagePreview.setImageURI(uri);
                    ivImagePreview.setVisibility(View.VISIBLE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_report);

        etReportMessage = findViewById(R.id.etReportMessage);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);
        btnAttachImage = findViewById(R.id.btnAttachImage);
        ivImagePreview = findViewById(R.id.ivImagePreview);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        setupBottomNav();

        btnAttachImage.setOnClickListener(v -> {
            animateClick(v);
            imagePickerLauncher.launch("image/*");
        });

        btnSubmitReport.setOnClickListener(v -> {
            animateClick(v);
            uploadData();
        });
    }

    private void uploadData() {
        String message = etReportMessage.getText().toString().trim();
        if (message.isEmpty()) {
            etReportMessage.setError("Please describe the issue");
            return;
        }

        btnSubmitReport.setEnabled(false);
        btnSubmitReport.setText("Uploading...");

        if (imageUri != null) {
            uploadImageToImgBB(message);
        } else {
            // Save without an image
            saveReportToDatabase(message, null);
        }
    }
`
    private void uploadImageToImgBB(String reportMessage) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            selectedImage.compress(Bitmap.CompressFormat.JPEG, 60, baos); // Compress image
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            OkHttpClient client = new OkHttpClient();
            RequestBody formBody = new FormBody.Builder()
                    .add("key", IMGBB_API_KEY)
                    .add("image", base64Image)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.imgbb.com/1/upload")
                    .post(formBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(UserReportActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                        btnSubmitReport.setEnabled(true);
                        btnSubmitReport.setText("Submit Report");
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String responseData = response.body().string();
                            JSONObject jsonObject = new JSONObject(responseData);
                            String imageUrl = jsonObject.getJSONObject("data").getString("url");

                            runOnUiThread(() -> saveReportToDatabase(reportMessage, imageUrl));

                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                Toast.makeText(UserReportActivity.this, "Error parsing image data", Toast.LENGTH_SHORT).show();
                                btnSubmitReport.setEnabled(true);
                                btnSubmitReport.setText("Submit Report");
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(UserReportActivity.this, "Server error uploading image", Toast.LENGTH_SHORT).show();
                            btnSubmitReport.setEnabled(true);
                            btnSubmitReport.setText("Submit Report");
                        });
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            btnSubmitReport.setEnabled(true);
            btnSubmitReport.setText("Submit Report");
        }
    }

    private void saveReportToDatabase(String message, String imageUrl) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        DatabaseReference db = FirebaseDatabase.getInstance().getReference("reports");
        String reportId = db.push().getKey();

        HashMap<String, Object> reportMap = new HashMap<>();
        reportMap.put("userId", userId);
        reportMap.put("message", message);
        if (imageUrl != null) {
            reportMap.put("imageUrl", imageUrl);
        }
        reportMap.put("timestamp", System.currentTimeMillis());

        if (reportId != null) {
            db.child(reportId).setValue(reportMap).addOnCompleteListener(task -> {
                btnSubmitReport.setEnabled(true);
                btnSubmitReport.setText("Submit Report");

                if (task.isSuccessful()) {
                    Toast.makeText(this, "Report submitted successfully", Toast.LENGTH_SHORT).show();
                    etReportMessage.setText("");
                    ivImagePreview.setVisibility(View.GONE);
                    imageUri = null;
                } else {
                    Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupBottomNav() {
        bottomNavigationView.setSelectedItemId(R.id.nav_report);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_surveys) {
                startActivity(new Intent(this, CompleteActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_report) {
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            return false;
        });
    }

    private void animateClick(View view) {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.95f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.95f, 1f);
        ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY).setDuration(200).start();
    }
}