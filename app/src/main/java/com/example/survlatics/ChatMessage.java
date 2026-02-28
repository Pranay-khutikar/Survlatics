package com.example.survlatics;

public class ChatMessage {

    // Making these final ensures the message can't be accidentally altered after creation
    private final String text;
    private final boolean isBot;

    public ChatMessage(String text, boolean isBot) {
        this.text = text;
        this.isBot = isBot;
    }

    public String getText() {
        return text;
    }

    public boolean isBot() {
        return isBot;
    }
}