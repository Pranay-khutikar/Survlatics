package com.example.survlatics;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
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
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);

            splineWebView.setBackgroundColor(Color.TRANSPARENT);
            splineWebView.setVerticalScrollBarEnabled(false);
            splineWebView.setHorizontalScrollBarEnabled(false);

            splineWebView.setWebViewClient(new WebViewClient());

            splineWebView.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });

            splineWebView.loadUrl("https://my.spline.design/devicecloudcopycopycopy-EFopqvvPBSm3Wu0SrOLmXZ0y/");
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
            animateClick(v);
            Intent intent = new Intent(this, AddSurveyActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }
}