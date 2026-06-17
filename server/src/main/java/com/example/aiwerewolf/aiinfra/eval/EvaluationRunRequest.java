package com.example.aiwerewolf.aiinfra.eval;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.lang.Nullable;

public record EvaluationRunRequest(
        @Nullable @Min(1) @Max(20) Integer gameCount,
        @Nullable String templateId
) {
    public int safeGameCount() {
        return gameCount == null ? 1 : gameCount;
    }

    public String safeTemplateId() {
        return templateId == null || templateId.isBlank() ? "7-standard" : templateId.strip();
    }
}
