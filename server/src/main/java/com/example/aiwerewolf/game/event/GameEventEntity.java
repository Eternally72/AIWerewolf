package com.example.aiwerewolf.game.event;

import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_events", indexes = @Index(name = "idx_events_room_round", columnList = "roomId,roundNumber"))
public class GameEventEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String roomId;
    @Column(nullable = false)
    private int roundNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private GamePhase phase;
    @Column(nullable = false, length = 80)
    private String eventType;
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payloadJson;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MemoryScope scope;
    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public GamePhase getPhase() { return phase; }
    public void setPhase(GamePhase phase) { this.phase = phase; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public MemoryScope getScope() { return scope; }
    public void setScope(MemoryScope scope) { this.scope = scope; }
    public Instant getCreatedAt() { return createdAt; }
}
