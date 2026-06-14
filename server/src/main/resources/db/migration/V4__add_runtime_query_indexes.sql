CREATE INDEX idx_players_room_alive_type ON players(roomId, alive, type);
CREATE INDEX idx_players_room_role ON players(roomId, role);
CREATE INDEX idx_actions_room_round_type_resolved ON game_actions(roomId, roundNumber, actionType, resolved);
CREATE INDEX idx_actions_target ON game_actions(targetPlayerId);
CREATE INDEX idx_votes_room_round_target ON votes(roomId, roundNumber, targetPlayerId);
CREATE INDEX idx_memory_room_round_created ON memory_entries(roomId, roundNumber, createdAt);
CREATE INDEX idx_speeches_room_round_created ON speeches(roomId, roundNumber, createdAt);
