package com.example.aiwerewolf.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.aiwerewolf.config.LlmProperties;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "bailian")
public class BailianLlmClient implements LlmClient {
    private final LlmProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final MockLlmClient fallback = new MockLlmClient();

    public BailianLlmClient(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBailian().getBaseUrl())
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        String apiKey = properties.getBailian().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return fallback.complete(systemPrompt, userPrompt);
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", properties.getBailian().getModel(),
                    "temperature", 0.7,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)));
            String response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return extractContent(response);
        } catch (RuntimeException ex) {
            return fallback.complete(systemPrompt, userPrompt);
        }
    }

    private String extractContent(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isTextual() && !content.asText().isBlank()) {
                return content.asText();
            }
        } catch (Exception ex) {
            return response;
        }
        return response;
    }
}
