package com.example.aiwerewolf.game.view;

import com.example.aiwerewolf.speech.entity.SpeechEntity;
import org.springframework.lang.Nullable;

import java.time.Instant;

public record SpeechView(String playerId, int roundNumber, String content, @Nullable String claimedRole, Instant createdAt) {
    public static SpeechView of(SpeechEntity speech) {
        return new SpeechView(
                speech.getPlayerId(),
                speech.getRoundNumber(),
                speech.getContent(),
                speech.getClaimedRole() == null ? null : speech.getClaimedRole().name(),
                speech.getCreatedAt()
        );
    }
}
