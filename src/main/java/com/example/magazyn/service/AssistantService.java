package com.example.magazyn.service;

import com.example.magazyn.dto.AssistantRequest;
import com.example.magazyn.dto.AssistantResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final String apiKey;
    private final String model;
    private final String apiUrl;
    private final String systemPrompt;
    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    public AssistantService(
            @Value("${assistant.api-key:}") String apiKey,
            @Value("${assistant.model:deepseek-chat}") String model,
            @Value("${assistant.api-url:https://api.deepseek.com}") String apiUrl,
            @Value("classpath:assistant/system-prompt.md") Resource systemPromptResource) throws IOException {
        this.apiKey = apiKey;
        this.model = model;
        this.apiUrl = apiUrl;
        this.systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
        this.mapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public AssistantResponse chat(AssistantRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Assistant API key not configured");
            return new AssistantResponse("Asystent nie jest skonfigurowany. Skontaktuj się z administratorem.");
        }

        try {
            // Build the messages array
            List<ObjectNode> messages = new ArrayList<>();

            // System prompt
            messages.add(mapper.createObjectNode()
                    .put("role", "system")
                    .put("content", systemPrompt));

            // Conversation history
            if (request.getHistory() != null) {
                for (AssistantRequest.ChatMessage msg : request.getHistory()) {
                    messages.add(mapper.createObjectNode()
                            .put("role", msg.getRole())
                            .put("content", msg.getContent()));
                }
            }

            // Context tab hint
            String userMessage = request.getMessage();
            if (request.getContextTab() != null && !request.getContextTab().isBlank()
                    && !userMessage.toLowerCase().contains(request.getContextTab().toLowerCase())) {
                userMessage = "[Użytkownik jest na zakładce: " + translateTab(request.getContextTab()) + "] " + userMessage;
            }

            // Current user message
            messages.add(mapper.createObjectNode()
                    .put("role", "user")
                    .put("content", userMessage));

            // Build request body
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.set("messages", mapper.valueToTree(messages));
            body.put("temperature", 0.7);
            body.put("max_tokens", 1024);
            body.put("stream", false);

            String jsonBody = mapper.writeValueAsString(body);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("DeepSeek API error: {} - {}", response.statusCode(), response.body());
                return new AssistantResponse("Przepraszam, wystąpił błąd podczas komunikacji z asystentem. Spróbuj ponownie za chwilę.");
            }

            JsonNode responseJson = mapper.readTree(response.body());
            String reply = responseJson.path("choices").get(0).path("message").path("content").asText();

            return new AssistantResponse(reply);

        } catch (IOException e) {
            log.error("Failed to parse request or response", e);
            return new AssistantResponse("Przepraszam, wystąpił błąd przetwarzania. Spróbuj ponownie.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Request interrupted", e);
            return new AssistantResponse("Przepraszam, połączenie zostało przerwane.");
        }
    }

    private String translateTab(String tab) {
        return switch (tab) {
            case "dashboard" -> "Panel główny";
            case "products" -> "Produkty";
            case "locations" -> "Lokalizacje";
            case "documents" -> "Dokumenty";
            case "invoices" -> "Faktury";
            case "scanner" -> "Skaner";
            case "inventory" -> "Inwentaryzacja";
            case "settings" -> "Ustawienia";
            default -> tab;
        };
    }
}
