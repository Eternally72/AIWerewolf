package com.example.aiwerewolf.aiinfra.run;

import com.example.aiwerewolf.game.phase.GamePhase;
import org.springframework.lang.Nullable;

import java.time.Instant;

public record AgentRunResponse(
        String id,
        String roomId,
        String playerId,
        String agentId,
        int roundNumber,
        GamePhase phase,
        AgentRunPurpose purpose,
        AgentRunStatus status,
        boolean fallbackUsed,
        int attemptCount,
        long latencyMillis,
        String promptVersion,
        String taskPromptVersion,
        String modelProvider,
        @Nullable String modelName,
        @Nullable String rawOutput,
        @Nullable String parsedOutputJson,
        @Nullable String errorMessage,
        Instant createdAt
) {
    public static AgentRunResponse fromEntity(AgentRunEntity entity) {
        return new AgentRunResponse(
                entity.getId(),
                entity.getRoomId(),
                entity.getPlayerId(),
                entity.getAgentId(),
                entity.getRoundNumber(),
                entity.getPhase(),
                entity.getPurpose(),
                entity.getStatus(),
                entity.isFallbackUsed(),
                entity.getAttemptCount(),
                entity.getLatencyMillis(),
                entity.getPromptVersion(),
                entity.getTaskPromptVersion(),
                entity.getModelProvider(),
                entity.getModelName(),
                entity.getRawOutput(),
                entity.getParsedOutputJson(),
                entity.getErrorMessage(),
                entity.getCreatedAt());
    }
}
