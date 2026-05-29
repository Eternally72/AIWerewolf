package com.example.aiwerewolf.action.service;

import com.example.aiwerewolf.action.entity.ActionType;
import jakarta.validation.constraints.NotNull;

public record GameActionRequest(@NotNull ActionType actionType, String targetPlayerId, String secondaryTargetPlayerId, String reason) {
}
