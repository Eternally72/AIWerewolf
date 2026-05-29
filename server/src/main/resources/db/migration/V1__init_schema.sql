CREATE TABLE rooms (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    phase VARCHAR(40) NOT NULL,
    totalSeats INT NOT NULL,
    currentRound INT NOT NULL DEFAULT 1,
    humanMode VARCHAR(40) NOT NULL,
    observerViewMode VARCHAR(40) NOT NULL,
    createdAt TIMESTAMP(6) NOT NULL,
    updatedAt TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE game_configs (
    id VARCHAR(36) PRIMARY KEY,
    roomId VARCHAR(36) NOT NULL UNIQUE,
    roleConfigJson TEXT NOT NULL,
    ruleConfigJson TEXT NOT NULL,
    uiConfigJson TEXT NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE players (
    id VARCHAR(36) PRIMARY KEY,
    roomId VARCHAR(36) NOT NULL,
    name VARCHAR(80) NOT NULL,
    type VARCHAR(30) NOT NULL,
    role VARCHAR(40),
    camp VARCHAR(30),
    roleCategory VARCHAR(30),
    seatNumber INT NOT NULL,
    alive BOOLEAN NOT NULL,
    deathReason VARCHAR(50) NOT NULL,
    deathRound INT,
    host BOOLEAN NOT NULL,
    observer BOOLEAN NOT NULL,
    canVote BOOLEAN NOT NULL,
    canSpeak BOOLEAN NOT NULL,
    createdAt TIMESTAMP(6) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE rounds (
    id VARCHAR(36) PRIMARY KEY,
    roomId VARCHAR(36) NOT NULL,
    roundNumber INT NOT NULL,
    phase VARCHAR(50) NOT NULL,
    startedAt TIMESTAMP(6) NOT NULL,
    endedAt TIMESTAMP(6)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE game_actions (
    id VARCHAR(36) PRIMARY KEY,
    roomId VARCHAR(36) NOT NULL,
    roundNumber INT NOT NULL,
    phase VARCHAR(50) NOT NULL,
    actorPlayerId VARCHAR(36) NOT NULL,
    actionType VARCHAR(50) NOT NULL,
    targetPlayerId VARCHAR(36),
    secondaryTargetPlayerId VARCHAR(36),
    scope VARCHAR(40) NOT NULL,
    resultJson TEXT,
    resolved BOOLEAN NOT NULL,
    createdAt TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_action_once UNIQUE (roomId, roundNumber, phase, actorPlayerId, actionType)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE speeches (
    id VARCHAR(36) PRIMARY KEY,
    roomId VARCHAR(36) NOT NULL,
    roundNumber INT NOT NULL,
    playerId VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    claimedRole VARCHAR(40),
    publicVisible BOOLEAN NOT NULL,
    createdAt TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_speech_once UNIQUE (roomId, roundNumber, playerId)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE votes (
    id VARCHAR(36) PRIMARY KEY,
    roomId VARCHAR(36) NOT NULL,
    roundNumber INT NOT NULL,
    voterPlayerId VARCHAR(36) NOT NULL,
    targetPlayerId VARCHAR(36) NOT NULL,
    reason VARCHAR(500),
    createdAt TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_vote_once UNIQUE (roomId, roundNumber, voterPlayerId)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE memory_entries (
    id VARCHAR(36) PRIMARY KEY,
    roomId VARCHAR(36) NOT NULL,
    roundNumber INT NOT NULL,
    phase VARCHAR(50) NOT NULL,
    scope VARCHAR(40) NOT NULL,
    ownerPlayerId VARCHAR(36),
    visibleToPlayerIds TEXT,
    eventType VARCHAR(80) NOT NULL,
    content TEXT NOT NULL,
    metadataJson TEXT,
    createdAt TIMESTAMP(6) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE game_events (
    id VARCHAR(36) PRIMARY KEY,
    roomId VARCHAR(36) NOT NULL,
    roundNumber INT NOT NULL,
    phase VARCHAR(50) NOT NULL,
    eventType VARCHAR(80) NOT NULL,
    payloadJson TEXT NOT NULL,
    scope VARCHAR(40) NOT NULL,
    createdAt TIMESTAMP(6) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE agent_profiles (
    id VARCHAR(36) PRIMARY KEY,
    playerId VARCHAR(36) NOT NULL UNIQUE,
    personality VARCHAR(120) NOT NULL,
    riskPreference VARCHAR(40) NOT NULL,
    speechStyle VARCHAR(80) NOT NULL,
    strategyStyle VARCHAR(80) NOT NULL,
    createdAt TIMESTAMP(6) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE role_states (
    id VARCHAR(36) PRIMARY KEY,
    roomId VARCHAR(36) NOT NULL,
    playerId VARCHAR(36) NOT NULL,
    role VARCHAR(40) NOT NULL,
    stateJson TEXT NOT NULL,
    createdAt TIMESTAMP(6) NOT NULL,
    updatedAt TIMESTAMP(6) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE lover_relations (
    id VARCHAR(36) PRIMARY KEY,
    roomId VARCHAR(36) NOT NULL,
    playerAId VARCHAR(36) NOT NULL,
    playerBId VARCHAR(36) NOT NULL,
    thirdPartyMode BOOLEAN NOT NULL,
    createdAt TIMESTAMP(6) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
