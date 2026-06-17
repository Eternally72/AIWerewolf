CREATE TABLE agent_tasks (
    id VARCHAR(36) PRIMARY KEY,
    roomId VARCHAR(36) NOT NULL,
    playerId VARCHAR(36) NOT NULL,
    roundNumber INT NOT NULL,
    phase VARCHAR(50) NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    queuedAt TIMESTAMP(6) NOT NULL,
    startedAt TIMESTAMP(6),
    completedAt TIMESTAMP(6),
    latencyMillis BIGINT NOT NULL,
    errorMessage VARCHAR(500),
    createdAt TIMESTAMP(6) NOT NULL,
    updatedAt TIMESTAMP(6) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_agent_tasks_room_queued ON agent_tasks(roomId, queuedAt);
CREATE INDEX idx_agent_tasks_room_status ON agent_tasks(roomId, status);
CREATE INDEX idx_agent_tasks_player_queued ON agent_tasks(playerId, queuedAt);
CREATE INDEX idx_game_events_room_scope_created ON game_events(roomId, scope, createdAt);
