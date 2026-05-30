package com.example.aiwerewolf.agent.llm;

import com.example.aiwerewolf.config.LlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
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
        String baseUrl = Objects.requireNonNull(properties.getBailian().getBaseUrl(), "Bailian base URL must not be null");
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
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
            String requestJson = Objects.requireNonNull(objectMapper.writeValueAsString(body), "Bailian request JSON must not be null");
            String response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);
            return extractContent(response);
        } catch (Exception ex) {
            return fallback.complete(systemPrompt, userPrompt);
        }
    }

    private String extractContent(@Nullable String response) {
        if (response == null || response.isBlank()) {
            return fallback.complete("", "");
        }
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
