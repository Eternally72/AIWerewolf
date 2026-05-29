package com.example.aiwerewolf.role.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lover_relations", indexes = @Index(name = "idx_lovers_room", columnList = "roomId"))
public class LoverRelationEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String roomId;
    @Column(nullable = false)
    private String playerAId;
    @Column(nullable = false)
    private String playerBId;
    @Column(nullable = false)
    private boolean thirdPartyMode;
    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getPlayerAId() { return playerAId; }
    public void setPlayerAId(String playerAId) { this.playerAId = playerAId; }
    public String getPlayerBId() { return playerBId; }
    public void setPlayerBId(String playerBId) { this.playerBId = playerBId; }
    public boolean isThirdPartyMode() { return thirdPartyMode; }
    public void setThirdPartyMode(boolean thirdPartyMode) { this.thirdPartyMode = thirdPartyMode; }
}
