package com.example.aiwerewolf.game.event;

import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.memory.entity.MemoryScope;

import java.time.Instant;

public record GameEventResponse(
        String id,
        String roomId,
        int roundNumber,
        GamePhase phase,
        String eventType,
        String payloadJson,
        MemoryScope scope,
        Instant createdAt
) {
    public static GameEventResponse fromEntity(GameEventEntity entity) {
        return new GameEventResponse(
                entity.getId(),
                entity.getRoomId(),
                entity.getRoundNumber(),
                entity.getPhase(),
                entity.getEventType(),
                entity.getPayloadJson(),
                entity.getScope(),
                entity.getCreatedAt());
    }
}
