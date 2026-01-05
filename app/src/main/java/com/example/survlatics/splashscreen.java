package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class splashscreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashscreen);

        // Animate logo scale-in (1 second)
        ImageView logo = findViewById(R.id.imageView_logo);
        logo.animate().scaleX(1f).scaleY(1f).setDuration(1000);

        // Animate title fade-up (start after 400ms)
        TextView title = findViewById(R.id.textView_title);
        title.setAlpha(0f);
        title.animate().alpha(1f).translationYBy(-20f).setStartDelay(400).setDuration(600);

        // Animate subtitle fade (start after 700ms)
        TextView subtitle = findViewById(R.id.textView_subtitle);
        subtitle.setAlpha(0f);
        subtitle.animate().alpha(1f).setStartDelay(700).setDuration(600);

        // Optional: Show progress bar fade-in
        ProgressBar progress = findViewById(R.id.progressBar);
        if (progress != null) {
            progress.setAlpha(0f);
            progress.setVisibility(View.VISIBLE);
            progress.animate().alpha(1f).setStartDelay(1200).setDuration(400);
        }

        // Navigate after 3 seconds (your original timing)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(splashscreen.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, 3000);
    }
}
