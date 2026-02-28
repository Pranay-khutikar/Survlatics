package com.example.survlatics;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SurveyAdapter extends RecyclerView.Adapter<SurveyAdapter.ViewHolder> {

    private List<String> titles;
    private List<String> ids;
    private String statusText;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String surveyId);
    }

    public SurveyAdapter(List<String> titles, List<String> ids, String statusText, OnItemClickListener listener) {
        this.titles = titles;
        this.ids = ids;
        this.statusText = statusText;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_survey_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.titleTextView.setText(titles.get(position));

        // Format the status text neatly
        holder.statusTextView.setText(statusText);

        // Add soft tactile animation on card click before triggering the intent
        holder.itemView.setOnClickListener(v -> {
            animateClick(v);

            // Adding a tiny delay lets the animation play before the screen transitions
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                listener.onItemClick(ids.get(position));
            }, 150);
        });
    }

    @Override
    public int getItemCount() {
        return titles.size();
    }

    // Soft scale animation for card interactions
    private void animateClick(View view) {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.95f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.95f, 1f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY);
        animator.setDuration(200);
        animator.start();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView statusTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.surveyTitle);
            statusTextView = itemView.findViewById(R.id.surveyStatus);
        }
    }
}