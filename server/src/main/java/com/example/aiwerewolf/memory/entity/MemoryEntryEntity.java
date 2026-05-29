package com.example.aiwerewolf.memory.entity;

import com.example.aiwerewolf.game.phase.GamePhase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memory_entries", indexes = {
        @Index(name = "idx_memory_room_scope", columnList = "roomId,scope"),
        @Index(name = "idx_memory_owner", columnList = "ownerPlayerId")
})
public class MemoryEntryEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String roomId;
    @Column(nullable = false)
    private int roundNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private GamePhase phase;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MemoryScope scope;
    private String ownerPlayerId;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String visibleToPlayerIds;
    @Column(nullable = false, length = 80)
    private String eventType;
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String metadataJson;
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
    public MemoryScope getScope() { return scope; }
    public void setScope(MemoryScope scope) { this.scope = scope; }
    public String getOwnerPlayerId() { return ownerPlayerId; }
    public void setOwnerPlayerId(String ownerPlayerId) { this.ownerPlayerId = ownerPlayerId; }
    public String getVisibleToPlayerIds() { return visibleToPlayerIds; }
    public void setVisibleToPlayerIds(String visibleToPlayerIds) { this.visibleToPlayerIds = visibleToPlayerIds; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public Instant getCreatedAt() { return createdAt; }
}
