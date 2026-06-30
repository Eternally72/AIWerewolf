package com.example.aiwerewolf.aiinfra.prompt;

public record PromptBundle(
        String systemPrompt,
        String userPrompt,
        AgentPromptContext inputContext,
        String rolePromptVersion,
        String taskPromptVersion,
        String outputSchemaVersion
) {
}
