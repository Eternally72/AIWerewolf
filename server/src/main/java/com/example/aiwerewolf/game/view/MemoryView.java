package com.example.aiwerewolf.game.view;

import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.memory.entity.MemoryEntryEntity;
import com.example.aiwerewolf.memory.entity.MemoryScope;

import java.time.Instant;

public record MemoryView(
        String id,
        int roundNumber,
        GamePhase phase,
        MemoryScope scope,
        String eventType,
        String content,
        Instant createdAt
) {
    public static MemoryView of(MemoryEntryEntity entry) {
        return new MemoryView(
                entry.getId(),
                entry.getRoundNumber(),
                entry.getPhase(),
                entry.getScope(),
                entry.getEventType(),
                entry.getContent(),
                entry.getCreatedAt()
        );
    }
}
