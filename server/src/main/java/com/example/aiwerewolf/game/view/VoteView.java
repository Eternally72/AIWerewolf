package com.example.aiwerewolf.game.view;

import com.example.aiwerewolf.vote.entity.VoteEntity;

import java.time.Instant;

public record VoteView(String voterPlayerId, String targetPlayerId, String reason, Instant createdAt) {
    public static VoteView of(VoteEntity vote) {
        return new VoteView(vote.getVoterPlayerId(), vote.getTargetPlayerId(), vote.getReason(), vote.getCreatedAt());
    }
}
