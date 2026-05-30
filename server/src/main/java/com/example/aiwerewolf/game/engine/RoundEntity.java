package com.example.aiwerewolf.game.engine;

import com.example.aiwerewolf.game.phase.GamePhase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rounds", indexes = @Index(name = "idx_rounds_room_round", columnList = "roomId,roundNumber"))
public class RoundEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String roomId;
    @Column(nullable = false)
    private int roundNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private GamePhase phase;
    @Column(nullable = false)
    private Instant startedAt;
    private Instant endedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (startedAt == null) startedAt = Instant.now();
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
}
