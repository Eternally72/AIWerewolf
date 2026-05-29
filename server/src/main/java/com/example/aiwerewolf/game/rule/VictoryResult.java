package com.example.aiwerewolf.game.rule;

import com.example.aiwerewolf.role.model.Camp;

public record VictoryResult(boolean gameOver, Camp winner, String reason) {
    public static VictoryResult ongoing() {
        return new VictoryResult(false, null, "游戏继续");
    }

    public static VictoryResult win(Camp winner, String reason) {
        return new VictoryResult(true, winner, reason);
    }
}
