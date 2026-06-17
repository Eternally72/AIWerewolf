package com.example.aiwerewolf.aiinfra.gateway;

public interface ModelProvider {
    String name();

    String modelName();

    boolean available();

    String complete(String systemPrompt, String userPrompt);
}
