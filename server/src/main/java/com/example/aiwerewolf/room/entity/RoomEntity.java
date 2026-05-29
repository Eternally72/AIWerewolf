package com.example.aiwerewolf.room.entity;

import com.example.aiwerewolf.game.phase.GamePhase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rooms")
public class RoomEntity {
    @Id
    private String id;
    @Column(nullable = false, length = 120)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RoomStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GamePhase phase;
    @Column(nullable = false)
    private int totalSeats;
    @Column(nullable = false)
    private int currentRound = 1;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private HumanMode humanMode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ObserverViewMode observerViewMode;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = RoomStatus.WAITING;
        }
        if (phase == null) {
            phase = GamePhase.WAITING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public GamePhase getPhase() { return phase; }
    public void setPhase(GamePhase phase) { this.phase = phase; }
    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }
    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }
    public HumanMode getHumanMode() { return humanMode; }
    public void setHumanMode(HumanMode humanMode) { this.humanMode = humanMode; }
    public ObserverViewMode getObserverViewMode() { return observerViewMode; }
    public void setObserverViewMode(ObserverViewMode observerViewMode) { this.observerViewMode = observerViewMode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
