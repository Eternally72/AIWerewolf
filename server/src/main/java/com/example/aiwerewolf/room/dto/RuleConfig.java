package com.example.aiwerewolf.room.dto;

import com.example.aiwerewolf.game.rule.VictoryRule;

public record RuleConfig(
        VictoryRule victoryRule,
        boolean enableSheriff,
        boolean enableLastWords,
        boolean allowWitchSaveSelfFirstNight,
        boolean allowHunterShootWhenPoisoned,
        boolean allowGuardProtectSameTargetConsecutively,
        boolean allowWerewolfNightChat,
        boolean allowWhiteWolfKingExplode,
        boolean enableLovers,
        int speechTimeLimitSeconds,
        int voteTimeLimitSeconds,
        int nightActionTimeLimitSeconds,
        int aiThinkingDelayMillis,
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
                450,
                true,
                false
        );
    }
}
