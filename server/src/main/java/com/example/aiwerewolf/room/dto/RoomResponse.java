package com.example.aiwerewolf.room.dto;

import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.room.entity.HumanMode;
import com.example.aiwerewolf.room.entity.ObserverViewMode;
import com.example.aiwerewolf.room.entity.RoomStatus;
import org.springframework.lang.Nullable;

import java.time.Instant;

public record RoomResponse(
        String id,
        String name,
        RoomStatus status,
        GamePhase phase,
        int totalSeats,
        HumanMode humanMode,
        ObserverViewMode observerViewMode,
        Instant createdAt,
        Instant updatedAt,
        @Nullable String godViewToken
) {
}
