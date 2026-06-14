package com.example.aiwerewolf.game.engine;

import com.example.aiwerewolf.action.service.DeathResolutionService;
import com.example.aiwerewolf.action.service.NightActionService;
import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.runtime.GameRuntimeStateCache;
import com.example.aiwerewolf.game.runtime.IdempotencyService;
import com.example.aiwerewolf.game.runtime.PhaseAdvanceLockService;
import com.example.aiwerewolf.game.rule.VictoryConditionService;
import com.example.aiwerewolf.game.rule.VictoryResult;
import com.example.aiwerewolf.game.rule.VictoryRule;
import com.example.aiwerewolf.memory.service.MemoryService;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.entity.PlayerType;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.room.entity.RoomEntity;
import com.example.aiwerewolf.room.entity.RoomStatus;
import com.example.aiwerewolf.room.repository.RoomRepository;
import com.example.aiwerewolf.speech.service.SpeechService;
import com.example.aiwerewolf.vote.service.VoteService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.time.Duration;

@Service
public class GamePhaseEngine {
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final NightActionService nightActionService;
    private final SpeechService speechService;
    private final VoteService voteService;
    private final DeathResolutionService deathResolutionService;
    private final VictoryConditionService victoryConditionService;
    private final MemoryService memoryService;
    private final PhaseAdvanceLockService phaseAdvanceLockService;
    private final IdempotencyService idempotencyService;
    private final GameRuntimeStateCache runtimeStateCache;

    public GamePhaseEngine(RoomRepository roomRepository,
                           PlayerRepository playerRepository,
                           NightActionService nightActionService,
                           SpeechService speechService,
                           VoteService voteService,
                           DeathResolutionService deathResolutionService,
                           VictoryConditionService victoryConditionService,
                           MemoryService memoryService,
                           PhaseAdvanceLockService phaseAdvanceLockService,
                           IdempotencyService idempotencyService,
                           GameRuntimeStateCache runtimeStateCache) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.nightActionService = nightActionService;
        this.speechService = speechService;
        this.voteService = voteService;
        this.deathResolutionService = deathResolutionService;
        this.victoryConditionService = victoryConditionService;
        this.memoryService = memoryService;
        this.phaseAdvanceLockService = phaseAdvanceLockService;
        this.idempotencyService = idempotencyService;
        this.runtimeStateCache = runtimeStateCache;
    }

    @Transactional
    public RoomEntity advancePhase(String roomId) {
        String safeRoomId = Objects.requireNonNull(roomId, "roomId must not be null");
        return phaseAdvanceLockService.withRoomLock(safeRoomId, () -> advancePhaseLocked(safeRoomId));
    }

    private RoomEntity advancePhaseLocked(String safeRoomId) {
        RoomEntity room = room(safeRoomId);
        if (room.getStatus() == RoomStatus.PAUSED) {
            throw new BusinessException("ILLEGAL_PHASE_OPERATION", "游戏已暂停");
        }
        String processKey = "idempotency:%s:%s:%s:process".formatted(safeRoomId, room.getCurrentRound(), room.getPhase());
        if (idempotencyService.markIfAbsent(processKey, Duration.ofHours(12))) {
            processCurrentPhase(room);
        }
        if (room.getPhase() != GamePhase.GAME_OVER) {
            room.setPhase(nextPhase(room.getPhase()));
            if (room.getPhase() == GamePhase.NIGHT) {
                room.setCurrentRound(room.getCurrentRound() + 1);
            }
            memoryService.appendPublicMemory(safeRoomId, room.getCurrentRound(), room.getPhase(), "PHASE_CHANGED", "阶段切换：" + room.getPhase());
        }
        RoomEntity saved = roomRepository.save(room);
        runtimeStateCache.put(saved);
        return saved;
    }

    @Transactional
    public RoomEntity advanceUntilHumanInputRequired(String roomId) {
        String safeRoomId = Objects.requireNonNull(roomId, "roomId must not be null");
        RoomEntity room = room(safeRoomId);
        int guard = 0;
        while (guard++ < 32 && room.getStatus() == RoomStatus.RUNNING && !humanInputRequired(room) && room.getPhase() != GamePhase.GAME_OVER) {
            room = advancePhase(safeRoomId);
        }
        return room;
    }

    @Transactional
    public RoomEntity advanceUntilGameOver(String roomId) {
        String safeRoomId = Objects.requireNonNull(roomId, "roomId must not be null");
        RoomEntity room = room(safeRoomId);
        int guard = 0;
        while (guard++ < 200 && room.getStatus() == RoomStatus.RUNNING && room.getPhase() != GamePhase.GAME_OVER) {
            room = advancePhase(safeRoomId);
        }
        if (room.getPhase() != GamePhase.GAME_OVER) {
            throw new BusinessException("SIMULATION_LIMIT_REACHED", "模拟步数达到上限，游戏尚未结束");
        }
        return room;
    }

    public void processCurrentPhase(RoomEntity room) {
        int round = room.getCurrentRound();
        switch (room.getPhase()) {
            case GUARD_ACTION, WEREWOLF_ACTION, SEER_ACTION, WITCH_ACTION, OTHER_NIGHT_ACTION ->
                    nightActionService.generateAiNightActions(room.getId(), round, room.getPhase());
            case NIGHT_RESOLUTION -> {
                nightActionService.resolveNightActions(room.getId(), round);
                checkVictory(room);
            }
            case DAY_SPEECH -> speechService.generateAiSpeech(room.getId(), round);
            case DAY_VOTE -> voteService.generateAiVotes(room.getId(), round);
            case EXECUTION -> {
                Optional<String> targetId = voteService.calculateVoteResult(room.getId(), round);
                targetId.flatMap(this::findPlayer).ifPresent(player -> deathResolutionService.exilePlayer(player, room.getId(), round));
                checkVictory(room);
            }
            default -> {
            }
        }
    }

    public boolean validatePhaseTransition(GamePhase current, GamePhase next) {
        return nextPhase(current) == next;
    }

    private void checkVictory(RoomEntity room) {
        List<PlayerEntity> players = playerRepository.findByRoomIdOrderBySeatNumberAsc(room.getId());
        VictoryResult result = victoryConditionService.checkVictory(players, VictoryRule.SLAUGHTER_SIDE);
        if (result.gameOver()) {
            room.setPhase(GamePhase.GAME_OVER);
            room.setStatus(RoomStatus.GAME_OVER);
            memoryService.appendPublicMemory(room.getId(), room.getCurrentRound(), GamePhase.GAME_OVER, "GAME_OVER",
                    "游戏结束，胜利阵营：" + result.winner() + "，原因：" + result.reason());
        }
    }

    private boolean humanInputRequired(RoomEntity room) {
        return playerRepository.findByRoomIdAndAliveTrueOrderBySeatNumberAsc(room.getId()).stream()
                .filter(player -> player.getType() == PlayerType.HUMAN)
                .anyMatch(player -> humanCanActInPhase(player, room.getPhase()));
    }

    private boolean humanCanActInPhase(PlayerEntity player, GamePhase phase) {
        if (phase == GamePhase.DAY_SPEECH) {
            return player.isCanSpeak();
        }
        if (phase == GamePhase.DAY_VOTE) {
            return player.isCanVote();
        }
        Role role = player.getRole();
        if (role == null) {
            return false;
        }
        return switch (phase) {
            case WEREWOLF_ACTION -> role.isWerewolfCamp();
            case SEER_ACTION -> role == Role.SEER;
            case WITCH_ACTION -> role == Role.WITCH;
            case GUARD_ACTION -> role == Role.GUARD;
            default -> false;
        };
    }

    private GamePhase nextPhase(GamePhase phase) {
        return switch (phase) {
            case WAITING, ROLE_ASSIGNED -> GamePhase.FIRST_NIGHT;
            case FIRST_NIGHT, NIGHT -> GamePhase.GUARD_ACTION;
            case CUPID_ACTION -> GamePhase.GUARD_ACTION;
            case GUARD_ACTION -> GamePhase.WEREWOLF_ACTION;
            case WEREWOLF_ACTION -> GamePhase.SEER_ACTION;
            case SEER_ACTION -> GamePhase.WITCH_ACTION;
            case WITCH_ACTION -> GamePhase.OTHER_NIGHT_ACTION;
            case OTHER_NIGHT_ACTION -> GamePhase.NIGHT_RESOLUTION;
            case NIGHT_RESOLUTION -> GamePhase.DAY_ANNOUNCEMENT;
            case DAY_ANNOUNCEMENT -> GamePhase.LAST_WORDS;
            case LAST_WORDS -> GamePhase.DAY_SPEECH;
            case SHERIFF_ELECTION -> GamePhase.DAY_SPEECH;
            case DAY_SPEECH -> GamePhase.DAY_SKILL;
            case DAY_SKILL -> GamePhase.DAY_VOTE;
            case DAY_VOTE -> GamePhase.EXECUTION;
            case EXECUTION, HUNTER_SHOOT, WHITE_WOLF_KING_EXPLODE -> GamePhase.NIGHT;
            case GAME_OVER -> GamePhase.GAME_OVER;
        };
    }

    private Optional<PlayerEntity> findPlayer(@NonNull String playerId) {
        return playerRepository.findById(playerId);
    }

    private RoomEntity room(String roomId) {
        return roomRepository.findById(Objects.requireNonNull(roomId, "roomId must not be null"))
                .orElseThrow(() -> new BusinessException("ROOM_NOT_FOUND", "房间不存在"));
    }
}
