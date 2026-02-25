package com.example.survlatics;

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
    private String statusText; // Added to store "Pending" or "Completed"
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String surveyId);
    }

    // Updated to accept the statusText String
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

        // This line updates the status TextView on the card
        holder.statusTextView.setText("Status: " + statusText);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(ids.get(position)));
    }

    @Override
    public int getItemCount() {
        return titles.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView statusTextView; // Added for the status text

        public ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.surveyTitle);

            // Make sure R.id.surveyStatus matches the ID in your item_survey_card.xml
            statusTextView = itemView.findViewById(R.id.surveyStatus);
        }
    }
}