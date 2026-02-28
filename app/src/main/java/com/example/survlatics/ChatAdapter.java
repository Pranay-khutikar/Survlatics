package com.example.survlatics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> messageList;

    // ---> THIS IS THE MISSING PIECE THE ERROR IS COMPLAINING ABOUT <---
    public ChatAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);

        if (message.isBot()) {
            holder.tvBotMessage.setVisibility(View.VISIBLE);
            holder.tvBotMessage.setText(message.getText());
            holder.tvUserMessage.setVisibility(View.GONE);
        } else {
            holder.tvUserMessage.setVisibility(View.VISIBLE);
            holder.tvUserMessage.setText(message.getText());
            holder.tvBotMessage.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserMessage, tvBotMessage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserMessage = itemView.findViewById(R.id.tvUserMessage);
            tvBotMessage = itemView.findViewById(R.id.tvBotMessage);
        }
    }
}