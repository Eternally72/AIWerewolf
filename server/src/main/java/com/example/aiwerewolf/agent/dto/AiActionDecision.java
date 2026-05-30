package com.example.aiwerewolf.agent.dto;

import com.example.aiwerewolf.action.entity.ActionType;
import org.springframework.lang.Nullable;

public record AiActionDecision(
        ActionType actionType,
        @Nullable String targetPlayerId,
        @Nullable String secondaryTargetPlayerId,
        String reason
) {
}
