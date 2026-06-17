CREATE TABLE agent_runs (
    id VARCHAR(36) PRIMARY KEY,
    roomId VARCHAR(36) NOT NULL,
    playerId VARCHAR(36) NOT NULL,
    agentId VARCHAR(36) NOT NULL,
    roundNumber INT NOT NULL,
    phase VARCHAR(50) NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    fallbackUsed BOOLEAN NOT NULL,
    attemptCount INT NOT NULL,
    latencyMillis BIGINT NOT NULL,
    promptVersion VARCHAR(80) NOT NULL,
    taskPromptVersion VARCHAR(80) NOT NULL,
    modelProvider VARCHAR(80) NOT NULL,
    modelName VARCHAR(120),
    inputViewSnapshotJson LONGTEXT NOT NULL,
    rawOutput LONGTEXT,
    parsedOutputJson LONGTEXT,
    errorMessage VARCHAR(500),
    createdAt TIMESTAMP(6) NOT NULL,
    updatedAt TIMESTAMP(6) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_agent_runs_room_created ON agent_runs(roomId, createdAt);
CREATE INDEX idx_agent_runs_player_created ON agent_runs(playerId, createdAt);
CREATE INDEX idx_agent_runs_room_round_phase ON agent_runs(roomId, roundNumber, phase);
