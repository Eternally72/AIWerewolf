package com.example.aiwerewolf.agent.llm;

public interface LlmClient {
    String complete(String systemPrompt, String userPrompt);
}
