package com.example.aiwerewolf.aiinfra.gateway;

import com.example.aiwerewolf.aiinfra.observability.AiInfraMetrics;
import com.example.aiwerewolf.aiinfra.observability.AiInfraObservation;
import com.example.aiwerewolf.config.LlmProperties;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LlmGateway {
    private static final String MOCK_PROVIDER = "mock";

    private final LlmProperties properties;
    private final Map<String, ModelProvider> providers;
    private final AiInfraMetrics metrics;
    private final AiInfraObservation observation;

    public LlmGateway(LlmProperties properties,
                      List<ModelProvider> providers,
                      AiInfraMetrics metrics,
                      AiInfraObservation observation) {
        this.properties = properties;
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(provider -> normalize(provider.name()), Function.identity()));
        this.metrics = metrics;
        this.observation = observation;
    }

    public ModelCallResponse complete(ModelCallRequest request) {
        return observation.observe("aiwerewolf.llm.gateway", () -> completeObserved(request));
    }

    private ModelCallResponse completeObserved(ModelCallRequest request) {
        long startedAt = System.nanoTime();
        String requestedProviderName = normalize(properties.getProvider());
        ModelProvider requestedProvider = providers.get(requestedProviderName);
        ModelProvider fallbackProvider = providers.get(MOCK_PROVIDER);

        if (requestedProvider == null) {
            return record(request, callFallback(request, fallbackProvider, startedAt, 0,
                    "Model provider is not registered: " + requestedProviderName));
        }
        if (!requestedProvider.available()) {
            return record(request, callFallback(request, fallbackProvider, startedAt, 0,
                    "Model provider is not available: " + requestedProvider.name()));
        }

        try {
            String content = requestedProvider.complete(request.systemPrompt(), request.userPrompt());
            if (content == null || content.isBlank()) {
                return record(request, callFallback(request, fallbackProvider, startedAt, 1,
                        "Model provider returned empty content: " + requestedProvider.name()));
            }
            return record(request, response(content, requestedProvider, false, 1, startedAt, null));
        } catch (Exception ex) {
            return record(request, callFallback(request, fallbackProvider, startedAt, 1,
                    "Model provider failed: " + requestedProvider.name() + "; " + rootMessage(ex)));
        }
    }

    private ModelCallResponse callFallback(ModelCallRequest request,
                                           @Nullable ModelProvider fallbackProvider,
                                           long startedAt,
                                           int previousAttempts,
                                           String reason) {
        if (fallbackProvider == null) {
            return new ModelCallResponse("", MOCK_PROVIDER, "missing", true, previousAttempts,
                    elapsedMillis(startedAt), reason + "; mock provider is missing");
        }
        try {
            String content = fallbackProvider.complete(request.systemPrompt(), request.userPrompt());
            return response(content, fallbackProvider, true, previousAttempts + 1, startedAt, reason);
        } catch (Exception ex) {
            return new ModelCallResponse("", fallbackProvider.name(), fallbackProvider.modelName(), true,
                    previousAttempts + 1, elapsedMillis(startedAt), reason + "; fallback failed: " + rootMessage(ex));
        }
    }

    private ModelCallResponse response(String content,
                                       ModelProvider provider,
                                       boolean fallbackUsed,
                                       int attemptCount,
                                       long startedAt,
                                       @Nullable String errorMessage) {
        return new ModelCallResponse(
                content == null ? "" : content,
                provider.name(),
                provider.modelName(),
                fallbackUsed,
                attemptCount,
                elapsedMillis(startedAt),
                errorMessage);
    }

    private ModelCallResponse record(ModelCallRequest request, ModelCallResponse response) {
        metrics.recordLlmCall(
                request.purpose(),
                response.providerName(),
                response.modelName(),
                response.providerFallbackUsed(),
                response.providerAttemptCount(),
                response.latencyMillis());
        return response;
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private String normalize(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return MOCK_PROVIDER;
        }
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
