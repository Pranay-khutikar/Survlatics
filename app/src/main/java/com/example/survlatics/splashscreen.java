package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class splashscreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashscreen);

        // 1. Logo "Pop" animation using Overshoot
        ImageView logo = findViewById(R.id.imageView_logo);
        logo.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(new OvershootInterpolator())
                .setDuration(1000)
                .start();

        // 2. Title fade and gentle slide up
        TextView title = findViewById(R.id.textView_title);
        title.setTranslationY(30f);
        title.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(500)
                .setDuration(800)
                .start();

        // 3. Subtitle fade in
        TextView subtitle = findViewById(R.id.textView_subtitle);
        subtitle.animate()
                .alpha(1f)
                .setStartDelay(900)
                .setDuration(700)
                .start();

        // 4. Progress bar soft appearance
        ProgressBar progress = findViewById(R.id.progressBar);
        if (progress != null) {
            progress.animate()
                    .alpha(1f)
                    .setStartDelay(1400)
                    .setDuration(500)
                    .start();
        }

        // 5. Navigate with a smooth fade transition
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(splashscreen.this, MainActivity.class);
            startActivity(intent);

            // This is the key to that premium "smooth glide" feel
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

            finish();
        }, 3000);
    }
}