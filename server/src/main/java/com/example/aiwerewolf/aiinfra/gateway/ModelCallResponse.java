package com.example.aiwerewolf.aiinfra.gateway;

import org.springframework.lang.Nullable;

public record ModelCallResponse(
        String content,
        String providerName,
        String modelName,
        boolean providerFallbackUsed,
        int providerAttemptCount,
        long latencyMillis,
        @Nullable String errorMessage
) {
}
