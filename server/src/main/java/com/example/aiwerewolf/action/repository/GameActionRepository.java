package com.example.aiwerewolf.action.repository;

import com.example.aiwerewolf.action.entity.ActionType;
import com.example.aiwerewolf.action.entity.GameActionEntity;
import com.example.aiwerewolf.game.phase.GamePhase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameActionRepository extends JpaRepository<GameActionEntity, String> {
    List<GameActionEntity> findByRoomIdAndRoundNumberAndPhase(String roomId, int roundNumber, GamePhase phase);
    List<GameActionEntity> findByRoomIdAndRoundNumber(String roomId, int roundNumber);
    Optional<GameActionEntity> findByRoomIdAndRoundNumberAndPhaseAndActorPlayerIdAndActionType(
            String roomId, int roundNumber, GamePhase phase, String actorPlayerId, ActionType actionType);
    boolean existsByRoomIdAndRoundNumberAndPhaseAndActorPlayerId(
            String roomId, int roundNumber, GamePhase phase, String actorPlayerId);
}
