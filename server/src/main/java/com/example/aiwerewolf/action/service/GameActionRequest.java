package com.example.aiwerewolf.action.service;

import com.example.aiwerewolf.action.entity.ActionType;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

public record GameActionRequest(
        @NotNull ActionType actionType,
        @Nullable String targetPlayerId,
        @Nullable String secondaryTargetPlayerId,
        @Nullable String reason
) {
}
