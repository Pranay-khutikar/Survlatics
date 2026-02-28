package com.example.survlatics;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminActivity extends AppCompatActivity {

    private WebView splineWebView;
    private Button btnAddSurvey;
    private BottomNavigationView bottomNavigationView;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_admin);

        // --- 1. Initialize UI Components ---
        btnAddSurvey = findViewById(R.id.btnAddSurvey);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        splineWebView = findViewById(R.id.spline_webview_admin);

        // --- 2. The Bulletproof WebView Setup ---
        if (splineWebView != null) {
            WebSettings webSettings = splineWebView.getSettings();
            webSettings.setJavaScriptEnabled(true); // Required for 3D rendering
            webSettings.setDomStorageEnabled(true);

            // Make it look native: Transparent background & hide scrollbars
            splineWebView.setBackgroundColor(Color.TRANSPARENT);
            splineWebView.setVerticalScrollBarEnabled(false);
            splineWebView.setHorizontalScrollBarEnabled(false);

            // Keep the loading inside the app, not opening Chrome
            splineWebView.setWebViewClient(new WebViewClient());

            // --- Prevent Android from stealing the one-finger swipe ---
            splineWebView.setOnTouchListener((v, event) -> {
                // This tells the parent layout "Do not scroll the screen, let me spin the 3D model!"
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });

            // Use your public Spline Viewer link here!
            splineWebView.loadUrl("https://my.spline.design/devicecloudcopycopy-nO0H3D4CgGvXGUWJpCRAm9pw/");
        }

        // --- 3. Set Home as selected icon ---
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        // --- 4. Navigation Logic ---
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_surveys) {
                navigateTo(AdminSurveyListActivity.class);
                return true;
            }
            if (id == R.id.nav_account) {
                navigateTo(Accountadmin.class);
                return true;
            }
            return false;
        });

        // --- 5. Add Survey Action ---
        btnAddSurvey.setOnClickListener(v -> {
            startActivity(new Intent(this, AddSurveyActivity.class));
        });
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }
}