package com.example.aiwerewolf.aiinfra.worker;

import com.example.aiwerewolf.aiinfra.run.AgentRunPurpose;
import com.example.aiwerewolf.game.phase.GamePhase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_tasks", indexes = {
        @Index(name = "idx_agent_tasks_room_queued", columnList = "roomId,queuedAt"),
        @Index(name = "idx_agent_tasks_room_status", columnList = "roomId,status"),
        @Index(name = "idx_agent_tasks_player_queued", columnList = "playerId,queuedAt")
})
public class AgentTaskEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String roomId;
    @Column(nullable = false)
    private String playerId;
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
    private AgentTaskStatus status;
    @Column(nullable = false)
    private Instant queuedAt;
    private Instant startedAt;
    private Instant completedAt;
    @Column(nullable = false)
    private long latencyMillis;
    @Column(length = 500)
    private String errorMessage;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    public static AgentTaskEntity queued(AgentTaskRequest request) {
        AgentTaskEntity entity = new AgentTaskEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setRoomId(request.roomId());
        entity.setPlayerId(request.playerId());
        entity.setRoundNumber(request.roundNumber());
        entity.setPhase(request.phase());
        entity.setPurpose(request.purpose());
        entity.setStatus(AgentTaskStatus.QUEUED);
        entity.setQueuedAt(Instant.now());
        entity.setLatencyMillis(0);
        return entity;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        if (queuedAt == null) {
            queuedAt = now;
        }
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

    public void markRunning() {
        startedAt = Instant.now();
        status = AgentTaskStatus.RUNNING;
        latencyMillis = 0;
    }

    public void markSucceeded() {
        completedAt = Instant.now();
        status = AgentTaskStatus.SUCCEEDED;
        refreshLatency();
        errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        completedAt = Instant.now();
        status = AgentTaskStatus.FAILED;
        refreshLatency();
        this.errorMessage = truncate(errorMessage);
    }

    public void markTimedOut() {
        completedAt = Instant.now();
        status = AgentTaskStatus.TIMED_OUT;
        refreshLatency();
        errorMessage = "AI Agent 任务执行超时";
    }

    public AgentTaskSnapshot snapshot() {
        return new AgentTaskSnapshot(id, roomId, playerId, roundNumber, phase, purpose, status,
                queuedAt, startedAt, completedAt, latencyMillis, errorMessage);
    }

    public void refreshLatency() {
        Instant start = startedAt == null ? queuedAt : startedAt;
        Instant end = completedAt == null ? Instant.now() : completedAt;
        latencyMillis = Math.max(0, Duration.between(start, end).toMillis());
    }

    @Nullable
    private String truncate(@Nullable String value) {
        if (value == null || value.length() <= 500) {
            return value;
        }
        return value.substring(0, 500);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public GamePhase getPhase() { return phase; }
    public void setPhase(GamePhase phase) { this.phase = phase; }
    public AgentRunPurpose getPurpose() { return purpose; }
    public void setPurpose(AgentRunPurpose purpose) { this.purpose = purpose; }
    public AgentTaskStatus getStatus() { return status; }
    public void setStatus(AgentTaskStatus status) { this.status = status; }
    public Instant getQueuedAt() { return queuedAt; }
    public void setQueuedAt(Instant queuedAt) { this.queuedAt = queuedAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public long getLatencyMillis() { return latencyMillis; }
    public void setLatencyMillis(long latencyMillis) { this.latencyMillis = latencyMillis; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
