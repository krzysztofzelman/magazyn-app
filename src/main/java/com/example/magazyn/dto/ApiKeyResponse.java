package com.example.magazyn.dto;

public class ApiKeyResponse {
    private String apiKey;

    public ApiKeyResponse() {}

    public ApiKeyResponse(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
