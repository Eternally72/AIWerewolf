package com.example.aiwerewolf.room.dto;

import com.example.aiwerewolf.game.rule.VictoryRule;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RuleConfig(
        @NotNull VictoryRule victoryRule,
        boolean enableSheriff,
        boolean enableLastWords,
        boolean allowWitchSaveSelfFirstNight,
        boolean allowHunterShootWhenPoisoned,
        boolean allowGuardProtectSameTargetConsecutively,
        boolean allowWerewolfNightChat,
        boolean allowWhiteWolfKingExplode,
        boolean enableLovers,
        @Min(10) @Max(600) int speechTimeLimitSeconds,
        @Min(10) @Max(300) int voteTimeLimitSeconds,
        @Min(10) @Max(300) int nightActionTimeLimitSeconds,
        @Min(700) @Max(5000) int aiThinkingDelayMillis,
        boolean autoAdvance,
        boolean revealRoleOnDeath
) {
    public static RuleConfig defaults() {
        return new RuleConfig(
                VictoryRule.SLAUGHTER_SIDE,
                false,
                true,
                true,
                false,
                false,
                true,
                true,
                false,
                90,
                45,
                45,
                1200,
                true,
                false
        );
    }
}
