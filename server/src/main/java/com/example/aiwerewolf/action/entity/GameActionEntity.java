package com.example.aiwerewolf.action.entity;

import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_actions", indexes = {
        @Index(name = "idx_actions_room_phase", columnList = "roomId,roundNumber,phase"),
        @Index(name = "idx_actions_actor", columnList = "actorPlayerId")
}, uniqueConstraints = @UniqueConstraint(name = "uk_action_once", columnNames = {"roomId", "roundNumber", "phase", "actorPlayerId", "actionType"}))
public class GameActionEntity {
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
    private String actorPlayerId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ActionType actionType;
    private String targetPlayerId;
    private String secondaryTargetPlayerId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MemoryScope scope;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String resultJson;
    @Column(nullable = false)
    private boolean resolved;
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
    public String getActorPlayerId() { return actorPlayerId; }
    public void setActorPlayerId(String actorPlayerId) { this.actorPlayerId = actorPlayerId; }
    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }
    public String getTargetPlayerId() { return targetPlayerId; }
    public void setTargetPlayerId(String targetPlayerId) { this.targetPlayerId = targetPlayerId; }
    public String getSecondaryTargetPlayerId() { return secondaryTargetPlayerId; }
    public void setSecondaryTargetPlayerId(String secondaryTargetPlayerId) { this.secondaryTargetPlayerId = secondaryTargetPlayerId; }
    public MemoryScope getScope() { return scope; }
    public void setScope(MemoryScope scope) { this.scope = scope; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public Instant getCreatedAt() { return createdAt; }
}
