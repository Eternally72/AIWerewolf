package com.example.aiwerewolf.player.entity;

import com.example.aiwerewolf.role.model.Camp;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.role.model.RoleCategory;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "players", indexes = {
        @Index(name = "idx_players_room", columnList = "roomId"),
        @Index(name = "idx_players_room_seat", columnList = "roomId,seatNumber")
})
public class PlayerEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String roomId;
    @Column(nullable = false, length = 80)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PlayerType type;
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private Role role;
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Camp camp;
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private RoleCategory roleCategory;
    @Column(nullable = false)
    private int seatNumber;
    @Column(nullable = false)
    private boolean alive = true;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeathReason deathReason = DeathReason.NONE;
    private Integer deathRound;
    @Column(nullable = false)
    private boolean host;
    @Column(nullable = false)
    private boolean observer;
    @Column(nullable = false)
    private boolean canVote = true;
    @Column(nullable = false)
    private boolean canSpeak = true;
    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PlayerType getType() { return type; }
    public void setType(PlayerType type) { this.type = type; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Camp getCamp() { return camp; }
    public void setCamp(Camp camp) { this.camp = camp; }
    public RoleCategory getRoleCategory() { return roleCategory; }
    public void setRoleCategory(RoleCategory roleCategory) { this.roleCategory = roleCategory; }
    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public DeathReason getDeathReason() { return deathReason; }
    public void setDeathReason(DeathReason deathReason) { this.deathReason = deathReason; }
    public Integer getDeathRound() { return deathRound; }
    public void setDeathRound(Integer deathRound) { this.deathRound = deathRound; }
    public boolean isHost() { return host; }
    public void setHost(boolean host) { this.host = host; }
    public boolean isObserver() { return observer; }
    public void setObserver(boolean observer) { this.observer = observer; }
    public boolean isCanVote() { return canVote; }
    public void setCanVote(boolean canVote) { this.canVote = canVote; }
    public boolean isCanSpeak() { return canSpeak; }
    public void setCanSpeak(boolean canSpeak) { this.canSpeak = canSpeak; }
    public Instant getCreatedAt() { return createdAt; }
}
