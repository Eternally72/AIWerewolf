package com.example.aiwerewolf.aiinfra.gateway;

import com.example.aiwerewolf.config.LlmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ZhipuModelProvider extends OpenAiCompatibleProviderSupport {
    private final LlmProperties properties;

    public ZhipuModelProvider(LlmProperties properties, ObjectMapper objectMapper) {
        super(objectMapper);
        this.properties = properties;
    }

    @Override
    public String name() {
        return "zhipu";
    }

    @Override
    protected LlmProperties.ChatProvider config() {
        return properties.getZhipu();
    }
}
