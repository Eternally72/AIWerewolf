package com.example.aiwerewolf.aiinfra.run;

import com.example.aiwerewolf.game.phase.GamePhase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_runs", indexes = {
        @Index(name = "idx_agent_runs_room_created", columnList = "roomId,createdAt"),
        @Index(name = "idx_agent_runs_player_created", columnList = "playerId,createdAt"),
        @Index(name = "idx_agent_runs_room_round_phase", columnList = "roomId,roundNumber,phase")
})
public class AgentRunEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String roomId;
    @Column(nullable = false)
    private String playerId;
    @Column(nullable = false)
    private String agentId;
    @Column(nullable = false)
    private int roundNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private GamePhase phase;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AgentRunPurpose purpose;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AgentRunStatus status;
    @Column(nullable = false)
    private boolean fallbackUsed;
    @Column(nullable = false)
    private int attemptCount;
    @Column(nullable = false)
    private long latencyMillis;
    @Column(nullable = false, length = 80)
    private String promptVersion;
    @Column(nullable = false, length = 80)
    private String taskPromptVersion;
    @Column(nullable = false, length = 80)
    private String modelProvider;
    @Column(length = 120)
    private String modelName;
    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String inputViewSnapshotJson;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String rawOutput;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String parsedOutputJson;
    @Column(length = 500)
    private String errorMessage;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public GamePhase getPhase() { return phase; }
    public void setPhase(GamePhase phase) { this.phase = phase; }
    public AgentRunPurpose getPurpose() { return purpose; }
    public void setPurpose(AgentRunPurpose purpose) { this.purpose = purpose; }
    public AgentRunStatus getStatus() { return status; }
    public void setStatus(AgentRunStatus status) { this.status = status; }
    public boolean isFallbackUsed() { return fallbackUsed; }
    public void setFallbackUsed(boolean fallbackUsed) { this.fallbackUsed = fallbackUsed; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public long getLatencyMillis() { return latencyMillis; }
    public void setLatencyMillis(long latencyMillis) { this.latencyMillis = latencyMillis; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public String getTaskPromptVersion() { return taskPromptVersion; }
    public void setTaskPromptVersion(String taskPromptVersion) { this.taskPromptVersion = taskPromptVersion; }
    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getInputViewSnapshotJson() { return inputViewSnapshotJson; }
    public void setInputViewSnapshotJson(String inputViewSnapshotJson) { this.inputViewSnapshotJson = inputViewSnapshotJson; }
    public String getRawOutput() { return rawOutput; }
    public void setRawOutput(String rawOutput) { this.rawOutput = rawOutput; }
    public String getParsedOutputJson() { return parsedOutputJson; }
    public void setParsedOutputJson(String parsedOutputJson) { this.parsedOutputJson = parsedOutputJson; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
