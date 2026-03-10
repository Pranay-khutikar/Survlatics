package com.example.survlatics;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminReportListActivity extends AppCompatActivity {

    private RecyclerView recyclerReports;
    private BottomNavigationView bottomNavigationView;
    private List<ReportModel> reportList;
    private ReportAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_report_list); // Make sure you have this XML as provided earlier

        recyclerReports = findViewById(R.id.recyclerReports);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        recyclerReports.setLayoutManager(new LinearLayoutManager(this));
        reportList = new ArrayList<>();
        adapter = new ReportAdapter(reportList);
        recyclerReports.setAdapter(adapter);

        setupBottomNav();
        loadReports();
    }

    private void loadReports() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("reports");
        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reportList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String message = data.child("message").getValue(String.class);
                    String userId = data.child("userId").getValue(String.class);
                    String imageUrl = data.child("imageUrl").getValue(String.class);

                    if (message != null && userId != null) {
                        reportList.add(new ReportModel(userId, message, imageUrl));
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminReportListActivity.this, "Failed to load reports", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNav() {
        bottomNavigationView.setSelectedItemId(R.id.nav_report);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, AdminActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_surveys) {
                startActivity(new Intent(this, AdminSurveyListActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_report) {
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, Accountadmin.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            return false;
        });
    }

    // --- Model ---
    public static class ReportModel {
        String userId, message, imageUrl;
        public ReportModel(String userId, String message, String imageUrl) {
            this.userId = userId;
            this.message = message;
            this.imageUrl = imageUrl;
        }
    }

    // --- Adapter ---
    public static class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
        List<ReportModel> list;

        public ReportAdapter(List<ReportModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_card, parent, false);
            return new ReportViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
            ReportModel report = list.get(position);
            holder.tvUserId.setText("User ID: " + report.userId);
            holder.tvMessage.setText(report.message);

            if (report.imageUrl != null && !report.imageUrl.isEmpty()) {
                holder.ivImage.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext())
                        .load(report.imageUrl)
                        .into(holder.ivImage);
            } else {
                holder.ivImage.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ReportViewHolder extends RecyclerView.ViewHolder {
            TextView tvUserId, tvMessage;
            ImageView ivImage;

            public ReportViewHolder(@NonNull View itemView) {
                super(itemView);
                tvUserId = itemView.findViewById(R.id.tvUserId);
                tvMessage = itemView.findViewById(R.id.tvReportMessage);
                ivImage = itemView.findViewById(R.id.ivReportImage);
            }
        }
    }
}