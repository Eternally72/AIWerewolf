package com.example.aiwerewolf.game.engine;

import com.example.aiwerewolf.action.entity.ActionType;
import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.room.entity.RoomEntity;
import com.example.aiwerewolf.room.repository.RoomRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class GameOperationValidator {
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;

    public GameOperationValidator(RoomRepository roomRepository, PlayerRepository playerRepository) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
    }

    public RoomEntity requireRoom(String roomId) {
        return roomRepository.findById(Objects.requireNonNull(roomId, "roomId must not be null"))
                .orElseThrow(() -> new BusinessException("ROOM_NOT_FOUND", "房间不存在"));
    }

    public int currentRound(String roomId) {
        return requireRoom(roomId).getCurrentRound();
    }

    public PlayerEntity requirePlayerInRoom(String roomId, String playerId) {
        PlayerEntity player = playerRepository.findById(Objects.requireNonNull(playerId, "playerId must not be null"))
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_FOUND", "玩家不存在"));
        if (!roomId.equals(player.getRoomId())) {
            throw new BusinessException("PLAYER_NOT_FOUND", "玩家不属于当前房间");
        }
        return player;
    }

    public PlayerEntity requireAliveTargetInRoom(String roomId, @Nullable String targetPlayerId) {
        if (targetPlayerId == null || targetPlayerId.isBlank()) {
            throw new BusinessException("ILLEGAL_TARGET", "目标玩家不能为空");
        }
        PlayerEntity target = requirePlayerInRoom(roomId, targetPlayerId);
        if (!target.isAlive()) {
            throw new BusinessException("ILLEGAL_TARGET", "目标玩家已死亡");
        }
        return target;
    }

    public void requirePhase(String roomId, GamePhase expected) {
        GamePhase actual = requireRoom(roomId).getPhase();
        if (actual != expected) {
            throw new BusinessException("ILLEGAL_PHASE_OPERATION", "当前阶段是 " + actual + "，不能执行 " + expected + " 操作");
        }
    }

    public void requireNightActionPhase(String roomId, ActionType actionType) {
        GamePhase actual = requireRoom(roomId).getPhase();
        if (actionType == ActionType.NONE && isNightActionPhase(actual)) {
            return;
        }
        GamePhase expected = phaseFor(actionType);
        if (actual != expected) {
            throw new BusinessException("ILLEGAL_PHASE_OPERATION", "当前阶段是 " + actual + "，不能执行 " + actionType + " 行动");
        }
    }

    public GamePhase phaseFor(ActionType type) {
        return switch (type) {
            case KILL -> GamePhase.WEREWOLF_ACTION;
            case CHECK -> GamePhase.SEER_ACTION;
            case SAVE, POISON -> GamePhase.WITCH_ACTION;
            case GUARD -> GamePhase.GUARD_ACTION;
            case SWAP, LINK_LOVERS -> GamePhase.OTHER_NIGHT_ACTION;
            default -> GamePhase.NIGHT;
        };
    }

    private boolean isNightActionPhase(GamePhase phase) {
        return switch (phase) {
            case GUARD_ACTION, WEREWOLF_ACTION, SEER_ACTION, WITCH_ACTION, OTHER_NIGHT_ACTION -> true;
            default -> false;
        };
    }
}
