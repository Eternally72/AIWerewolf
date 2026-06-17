package com.example.aiwerewolf.aiinfra.gateway;

import com.example.aiwerewolf.config.LlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.Nullable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

abstract class OpenAiCompatibleProviderSupport implements ModelProvider {
    private final ObjectMapper objectMapper;

    protected OpenAiCompatibleProviderSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected abstract LlmProperties.ChatProvider config();

    @Override
    public String modelName() {
        return config().getModel();
    }

    @Override
    public boolean available() {
        return !isBlank(config().getApiKey())
                && !isBlank(config().getBaseUrl())
                && !isBlank(config().getModel());
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        LlmProperties.ChatProvider providerConfig = config();
        String apiKey = providerConfig.getApiKey();
        if (isBlank(apiKey)) {
            throw new IllegalStateException(name() + " API key is not configured");
        }
        if (isBlank(providerConfig.getBaseUrl())) {
            throw new IllegalStateException(name() + " base URL is not configured");
        }
        if (isBlank(providerConfig.getModel())) {
            throw new IllegalStateException(name() + " model is not configured");
        }

        try {
            Map<String, Object> body = Map.of(
                    "model", providerConfig.getModel(),
                    "temperature", 0.7,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)));
            String requestJson = Objects.requireNonNull(
                    objectMapper.writeValueAsString(body),
                    name() + " request JSON must not be null");
            String response = restClient(providerConfig)
                    .post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);
            return extractContent(response);
        } catch (Exception ex) {
            throw new IllegalStateException(name() + " model call failed", ex);
        }
    }

    private RestClient restClient(LlmProperties.ChatProvider providerConfig) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(providerConfig.getTimeoutSeconds());
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder()
                .baseUrl(providerConfig.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private String extractContent(@Nullable String response) {
        if (response == null || response.isBlank()) {
            throw new IllegalStateException(name() + " response is empty");
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isTextual() && !content.asText().isBlank()) {
                return content.asText();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse " + name() + " response", ex);
        }
        throw new IllegalStateException(name() + " response does not contain choices[0].message.content");
    }

    private boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
