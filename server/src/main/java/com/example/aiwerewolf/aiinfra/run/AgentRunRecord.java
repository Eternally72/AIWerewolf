package com.example.aiwerewolf.aiinfra.run;

import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.view.GameView;
import org.springframework.lang.Nullable;

public record AgentRunRecord(
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
        GameView inputViewSnapshot,
        @Nullable String rawOutput,
        @Nullable Object parsedOutput,
        @Nullable String errorMessage
) {
}
