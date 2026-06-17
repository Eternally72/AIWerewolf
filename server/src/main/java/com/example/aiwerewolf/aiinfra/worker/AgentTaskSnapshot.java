package com.example.aiwerewolf.aiinfra.worker;

import com.example.aiwerewolf.aiinfra.run.AgentRunPurpose;
import com.example.aiwerewolf.game.phase.GamePhase;

import java.time.Instant;

public record AgentTaskSnapshot(
        String taskId,
        String roomId,
        String playerId,
        int roundNumber,
        GamePhase phase,
        AgentRunPurpose purpose,
        AgentTaskStatus status,
        Instant queuedAt,
        Instant startedAt,
        Instant completedAt,
        long latencyMillis,
        String errorMessage
) {
}
