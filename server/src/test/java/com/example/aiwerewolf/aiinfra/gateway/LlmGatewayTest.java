package com.example.aiwerewolf.aiinfra.gateway;

import com.example.aiwerewolf.aiinfra.observability.AiInfraMetrics;
import com.example.aiwerewolf.aiinfra.observability.AiInfraObservation;
import com.example.aiwerewolf.aiinfra.run.AgentRunPurpose;
import com.example.aiwerewolf.config.LlmProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class LlmGatewayTest {
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    void routesToMockProviderByDefault() {
        LlmGateway gateway = gateway(new LlmProperties(), List.of(new MockModelProvider()));

        ModelCallResponse response = gateway.complete(request());

        assertThat(response.providerName()).isEqualTo("mock");
        assertThat(response.modelName()).isEqualTo("mock-json-v1");
        assertThat(response.providerFallbackUsed()).isFalse();
        assertThat(response.providerAttemptCount()).isEqualTo(1);
        assertThat(response.content()).contains("speech");
        assertThat(meterRegistry.counter("aiwerewolf.llm.calls",
                "purpose", "speech",
                "provider", "mock",
                "model", "mock-json-v1",
                "fallback", "false").count()).isEqualTo(1.0);
    }

    @Test
    void fallsBackToMockWhenConfiguredProviderIsUnavailable() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider("bailian");

        LlmGateway gateway = gateway(properties, List.of(unavailableProvider(), new MockModelProvider()));

        ModelCallResponse response = gateway.complete(request());

        assertThat(response.providerName()).isEqualTo("mock");
        assertThat(response.providerFallbackUsed()).isTrue();
        assertThat(response.providerAttemptCount()).isEqualTo(1);
        assertThat(response.errorMessage()).contains("not available");
    }

    @Test
    void fallsBackToMockWhenConfiguredProviderFails() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider("bailian");

        LlmGateway gateway = gateway(properties, List.of(failingProvider(), new MockModelProvider()));

        ModelCallResponse response = gateway.complete(request());

        assertThat(response.providerName()).isEqualTo("mock");
        assertThat(response.providerFallbackUsed()).isTrue();
        assertThat(response.providerAttemptCount()).isEqualTo(2);
        assertThat(response.errorMessage()).contains("Model provider failed");
    }

    @Test
    void supportsOpenAiCompatibleProviderNames() {
        LlmProperties properties = new LlmProperties();
        MockModelProvider mockProvider = new MockModelProvider();
        List<ModelProvider> providers = List.of(
                mockProvider,
                new GenericOpenAiCompatibleModelProvider(properties, new com.fasterxml.jackson.databind.ObjectMapper()),
                new BailianModelProvider(properties, new com.fasterxml.jackson.databind.ObjectMapper()),
                new DeepSeekModelProvider(properties, new com.fasterxml.jackson.databind.ObjectMapper()),
                new ZhipuModelProvider(properties, new com.fasterxml.jackson.databind.ObjectMapper()));

        Set<String> providerNames = providers.stream()
                .map(ModelProvider::name)
                .collect(Collectors.toSet());

        assertThat(providerNames).containsExactlyInAnyOrder(
                "mock",
                "openai-compatible",
                "bailian",
                "deepseek",
                "zhipu");
    }

    @Test
    void fallsBackToMockWhenDeepSeekHasNoApiKey() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider("deepseek");
        LlmGateway gateway = gateway(properties, List.of(
                new DeepSeekModelProvider(properties, new com.fasterxml.jackson.databind.ObjectMapper()),
                new MockModelProvider()));

        ModelCallResponse response = gateway.complete(request());

        assertThat(response.providerName()).isEqualTo("mock");
        assertThat(response.providerFallbackUsed()).isTrue();
        assertThat(response.errorMessage()).contains("not available");
    }

    @Test
    void fallsBackToMockWhenZhipuHasNoApiKey() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider("zhipu");
        LlmGateway gateway = gateway(properties, List.of(
                new ZhipuModelProvider(properties, new com.fasterxml.jackson.databind.ObjectMapper()),
                new MockModelProvider()));

        ModelCallResponse response = gateway.complete(request());

        assertThat(response.providerName()).isEqualTo("mock");
        assertThat(response.providerFallbackUsed()).isTrue();
        assertThat(response.errorMessage()).contains("not available");
    }

    private ModelCallRequest request() {
        return new ModelCallRequest("room-1", "player-1", AgentRunPurpose.SPEECH, "system", "user");
    }

    private LlmGateway gateway(LlmProperties properties, List<ModelProvider> providers) {
        return new LlmGateway(
                properties,
                providers,
                new AiInfraMetrics(meterRegistry),
                new AiInfraObservation(ObservationRegistry.NOOP));
    }

    private ModelProvider unavailableProvider() {
        return new ModelProvider() {
            @Override
            public String name() {
                return "bailian";
            }

            @Override
            public String modelName() {
                return "qwen-plus";
            }

            @Override
            public boolean available() {
                return false;
            }

            @Override
            public String complete(String systemPrompt, String userPrompt) {
                return "{}";
            }
        };
    }

    private ModelProvider failingProvider() {
        return new ModelProvider() {
            @Override
            public String name() {
                return "bailian";
            }

            @Override
            public String modelName() {
                return "qwen-plus";
            }

            @Override
            public boolean available() {
                return true;
            }

            @Override
            public String complete(String systemPrompt, String userPrompt) {
                throw new IllegalStateException("boom");
            }
        };
    }
}
