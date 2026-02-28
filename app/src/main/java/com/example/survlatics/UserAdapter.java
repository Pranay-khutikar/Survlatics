package com.example.survlatics;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<user> userList;
    private final Context context;
    private final FirebaseFirestore firestore;

    public UserAdapter(List<user> userList, Context context) {
        this.userList = userList;
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflating the item_user layout we styled earlier
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        user user = userList.get(position);

        holder.tvEmail.setText(user.getEmail());
        holder.tvRole.setText(user.getRole());

        holder.btnDelete.setOnClickListener(v -> {
            animateClick(v);
            showDeleteConfirmation(user, holder.getAdapterPosition());
        });
    }

    // --- Soft Scale Animation for Tactile Feedback ---
    private void animateClick(View view) {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.90f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.90f, 1f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY);
        animator.setDuration(200);
        animator.start();
    }

    // --- Mindful Deletion: Confirmation Dialog ---
    private void showDeleteConfirmation(user user, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Remove User")
                .setMessage("Are you sure you want to remove " + user.getEmail() + "? This action cannot be undone.")
                .setPositiveButton("Remove", (dialog, which) -> deleteUser(user, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUser(user user, int position) {
        firestore.collection("Users")
                .document(user.getUid())
                .delete()
                .addOnSuccessListener(unused -> {
                    if (position != RecyclerView.NO_POSITION && position < userList.size()) {
                        userList.remove(position);
                        notifyItemRemoved(position);
                        // Notify range changed to update positions of remaining items
                        notifyItemRangeChanged(position, userList.size());
                        Toast.makeText(context, "User access revoked", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Failed to remove user. Please check connection.", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmail, tvRole;
        Button btnDelete;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvRole = itemView.findViewById(R.id.tvRole);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}