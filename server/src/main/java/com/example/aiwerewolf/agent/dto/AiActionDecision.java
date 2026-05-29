package com.example.aiwerewolf.agent.dto;

import com.example.aiwerewolf.action.entity.ActionType;

public record AiActionDecision(ActionType actionType, String targetPlayerId, String secondaryTargetPlayerId, String reason) {
}
