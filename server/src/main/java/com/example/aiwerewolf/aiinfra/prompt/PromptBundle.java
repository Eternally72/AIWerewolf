package com.example.aiwerewolf.aiinfra.prompt;

public record PromptBundle(
        String systemPrompt,
        String userPrompt,
        String rolePromptVersion,
        String taskPromptVersion,
        String outputSchemaVersion
) {
}
