package com.example.aiwerewolf.vote.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "votes", indexes = @Index(name = "idx_votes_room_round", columnList = "roomId,roundNumber"),
        uniqueConstraints = @UniqueConstraint(name = "uk_vote_once", columnNames = {"roomId", "roundNumber", "voterPlayerId"}))
public class VoteEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String roomId;
    @Column(nullable = false)
    private int roundNumber;
    @Column(nullable = false)
    private String voterPlayerId;
    @Column(nullable = false)
    private String targetPlayerId;
    @Column(length = 500)
    private String reason;
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
    public String getVoterPlayerId() { return voterPlayerId; }
    public void setVoterPlayerId(String voterPlayerId) { this.voterPlayerId = voterPlayerId; }
    public String getTargetPlayerId() { return targetPlayerId; }
    public void setTargetPlayerId(String targetPlayerId) { this.targetPlayerId = targetPlayerId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getCreatedAt() { return createdAt; }
}
