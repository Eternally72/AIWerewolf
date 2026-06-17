package com.example.aiwerewolf.aiinfra.eval;

import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.room.entity.RoomStatus;
import org.springframework.lang.Nullable;

public record EvaluationGameResult(
        int index,
        @Nullable String roomId,
        boolean completed,
        @Nullable RoomStatus status,
        @Nullable GamePhase phase,
        int roundNumber,
        long durationMillis,
        int agentRunCount,
        int fallbackRunCount,
        int invalidDecisionFallbackCount,
        int leakageCount,
        @Nullable String errorMessage
) {
}
