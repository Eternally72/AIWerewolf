package com.example.aiwerewolf.aiinfra.worker;

import com.example.aiwerewolf.aiinfra.run.AgentRunPurpose;
import com.example.aiwerewolf.game.phase.GamePhase;

public record AgentTaskRequest(
        String roomId,
        String playerId,
        int roundNumber,
        GamePhase phase,
        AgentRunPurpose purpose
) {
}
