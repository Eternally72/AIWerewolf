package com.example.aiwerewolf.speech.entity;

import com.example.aiwerewolf.role.model.Role;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "speeches", indexes = @Index(name = "idx_speeches_room_round", columnList = "roomId,roundNumber"),
        uniqueConstraints = @UniqueConstraint(name = "uk_speech_once", columnNames = {"roomId", "roundNumber", "playerId"}))
public class SpeechEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String roomId;
    @Column(nullable = false)
    private int roundNumber;
    @Column(nullable = false)
    private String playerId;
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private Role claimedRole;
    @Column(nullable = false)
    private boolean publicVisible = true;
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
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Role getClaimedRole() { return claimedRole; }
    public void setClaimedRole(Role claimedRole) { this.claimedRole = claimedRole; }
    public boolean isPublicVisible() { return publicVisible; }
    public void setPublicVisible(boolean publicVisible) { this.publicVisible = publicVisible; }
    public Instant getCreatedAt() { return createdAt; }
}
