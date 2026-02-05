package com.example.survlatics;

import android.content.Context;
import android.widget.Toast;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<user> userList;
    private Context context;
    private FirebaseFirestore firestore;

    public UserAdapter(List<user> userList, Context context) {
        this.userList = userList;
        this.context = context;
        firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_user, parent, false);

        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {

        user user = userList.get(position);

        holder.tvEmail.setText(user.getEmail());
        holder.tvRole.setText("Role: " + user.getRole());

        holder.btnDelete.setOnClickListener(v -> {

            firestore.collection("Users")
                    .document(user.getUid())
                    .delete()
                    .addOnSuccessListener(unused -> {

                        userList.remove(position);
                        notifyItemRemoved(position);

                        Toast.makeText(context, "User removed", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show());
        });
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
