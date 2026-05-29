package com.example.aiwerewolf.room.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "game_configs", indexes = @Index(name = "idx_game_configs_room", columnList = "roomId"))
public class GameConfigEntity {
    @Id
    private String id;
    @Column(nullable = false, unique = true)
    private String roomId;
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String roleConfigJson;
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String ruleConfigJson;
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String uiConfigJson;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getRoleConfigJson() { return roleConfigJson; }
    public void setRoleConfigJson(String roleConfigJson) { this.roleConfigJson = roleConfigJson; }
    public String getRuleConfigJson() { return ruleConfigJson; }
    public void setRuleConfigJson(String ruleConfigJson) { this.ruleConfigJson = ruleConfigJson; }
    public String getUiConfigJson() { return uiConfigJson; }
    public void setUiConfigJson(String uiConfigJson) { this.uiConfigJson = uiConfigJson; }
}
