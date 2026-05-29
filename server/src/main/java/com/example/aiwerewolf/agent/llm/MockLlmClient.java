package com.example.aiwerewolf.agent.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(LlmClient.class)
public class MockLlmClient implements LlmClient {
    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return """
                {"speech":"我会基于公开信息发言，先观察票型和死亡信息，不轻易跟风。","strategySummary":"Mock 策略：优先选择存活且公开信息较少的目标。"}
                """;
    }
}
