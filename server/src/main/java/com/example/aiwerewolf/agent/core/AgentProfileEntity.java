package com.example.aiwerewolf.agent.core;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_profiles", indexes = @Index(name = "idx_agent_profiles_player", columnList = "playerId"))
public class AgentProfileEntity {
    @Id
    private String id;
    @Column(nullable = false, unique = true)
    private String playerId;
    @Column(nullable = false, length = 120)
    private String personality;
    @Column(nullable = false, length = 40)
    private String riskPreference;
    @Column(nullable = false, length = 80)
    private String speechStyle;
    @Column(nullable = false, length = 80)
    private String strategyStyle;
    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getPersonality() { return personality; }
    public void setPersonality(String personality) { this.personality = personality; }
    public String getRiskPreference() { return riskPreference; }
    public void setRiskPreference(String riskPreference) { this.riskPreference = riskPreference; }
    public String getSpeechStyle() { return speechStyle; }
    public void setSpeechStyle(String speechStyle) { this.speechStyle = speechStyle; }
    public String getStrategyStyle() { return strategyStyle; }
    public void setStrategyStyle(String strategyStyle) { this.strategyStyle = strategyStyle; }
    public Instant getCreatedAt() { return createdAt; }
}
