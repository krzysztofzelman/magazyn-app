package com.example.magazyn.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class AssistantRequest {

    @NotBlank(message = "Message is required")
    private String message;

    private List<ChatMessage> history;

    private String contextTab;

    public AssistantRequest() {}

    public AssistantRequest(String message, List<ChatMessage> history, String contextTab) {
        this.message = message;
        this.history = history;
        this.contextTab = contextTab;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<ChatMessage> getHistory() { return history; }
    public void setHistory(List<ChatMessage> history) { this.history = history; }

    public String getContextTab() { return contextTab; }
    public void setContextTab(String contextTab) { this.contextTab = contextTab; }

    public static class ChatMessage {
        private String role;
        private String content;

        public ChatMessage() {}

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
