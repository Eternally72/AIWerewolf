package com.example.aiwerewolf.action.service;

import com.example.aiwerewolf.action.entity.ActionType;
import com.example.aiwerewolf.action.entity.GameActionEntity;
import com.example.aiwerewolf.action.repository.GameActionRepository;
import com.example.aiwerewolf.agent.core.AiAgentService;
import com.example.aiwerewolf.agent.dto.AiActionDecision;
import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.view.GameViewBuilder;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import com.example.aiwerewolf.memory.service.MemoryService;
import com.example.aiwerewolf.player.entity.DeathReason;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.entity.PlayerType;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.role.model.Role;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class NightActionService {
    private final PlayerRepository playerRepository;
    private final GameActionRepository actionRepository;
    private final GameViewBuilder gameViewBuilder;
    private final AiAgentService aiAgentService;
    private final DeathResolutionService deathResolutionService;
    private final MemoryService memoryService;

    public NightActionService(PlayerRepository playerRepository,
                              GameActionRepository actionRepository,
                              GameViewBuilder gameViewBuilder,
                              AiAgentService aiAgentService,
                              DeathResolutionService deathResolutionService,
                              MemoryService memoryService) {
        this.playerRepository = playerRepository;
        this.actionRepository = actionRepository;
        this.gameViewBuilder = gameViewBuilder;
        this.aiAgentService = aiAgentService;
        this.deathResolutionService = deathResolutionService;
        this.memoryService = memoryService;
    }

    public void generateAiNightActions(String roomId, int round) {
        for (PlayerEntity player : playerRepository.findByRoomIdAndAliveTrueOrderBySeatNumberAsc(roomId)) {
            if (player.getType() != PlayerType.AI) {
                continue;
            }
            AiActionDecision decision = aiAgentService.decideNightAction(player.getId(), gameViewBuilder.buildPrivateView(roomId, player.getId()));
            if (decision.actionType() != ActionType.NONE) {
                if (requiresPrimaryTarget(decision.actionType()) && blank(decision.targetPlayerId())) {
                    continue;
                }
                saveAction(roomId, round, phaseFor(decision.actionType()), player.getId(), decision.actionType(),
                        decision.targetPlayerId(), decision.secondaryTargetPlayerId(), false);
            }
        }
    }

    public void submitHumanNightAction(String roomId, int round, String playerId, GameActionRequest request) {
        if (requiresPrimaryTarget(request.actionType()) && blank(request.targetPlayerId())) {
            throw new BusinessException("ILLEGAL_TARGET", "该行动必须选择目标玩家");
        }
        saveAction(roomId, round, phaseFor(request.actionType()), playerId, request.actionType(), request.targetPlayerId(), request.secondaryTargetPlayerId(), false);
    }

    public void resolveNightActions(String roomId, int round) {
        List<GameActionEntity> actions = actionRepository.findByRoomIdAndRoundNumber(roomId, round);
        Optional<String> guarded = actions.stream().filter(a -> a.getActionType() == ActionType.GUARD).map(this::targetId).flatMap(Optional::stream).findFirst();
        Optional<String> killed = actions.stream().filter(a -> a.getActionType() == ActionType.KILL).map(this::targetId).flatMap(Optional::stream).findFirst();
        Optional<String> poisoned = actions.stream().filter(a -> a.getActionType() == ActionType.POISON).map(this::targetId).flatMap(Optional::stream).findFirst();
        for (GameActionEntity action : actions.stream().filter(a -> a.getActionType() == ActionType.CHECK).toList()) {
            targetId(action).flatMap(this::findPlayer).ifPresent(target -> {
                String actorId = actorId(action);
                String result = target.getRole() == Role.HIDDEN_WOLF ? "GOOD" : target.getCamp().name();
                memoryService.appendPrivateMemory(roomId, round, GamePhase.SEER_ACTION, actorId,
                        "SEER_CHECK", "查验结果：" + target.getName() + " 是 " + result);
            });
        }
        killed.filter(killedId -> guarded.map(guardedId -> !killedId.equals(guardedId)).orElse(true))
                .flatMap(this::findPlayer)
                .ifPresent(target -> deathResolutionService.killPlayer(target, roomId, round, GamePhase.NIGHT_RESOLUTION, DeathReason.WEREWOLF_KILL));
        poisoned.ifPresent(playerId -> findPlayer(playerId).ifPresent(target ->
                deathResolutionService.killPlayer(target, roomId, round, GamePhase.NIGHT_RESOLUTION, DeathReason.WITCH_POISON)));
        actions.forEach(a -> {
            a.setResolved(true);
            actionRepository.save(a);
        });
    }

    private void saveAction(String roomId, int round, GamePhase phase, String actorId, ActionType type,
                            @Nullable String targetId, @Nullable String secondaryTargetId, boolean resolved) {
        if (actionRepository.findByRoomIdAndRoundNumberAndPhaseAndActorPlayerIdAndActionType(roomId, round, phase, actorId, type).isPresent()) {
            return;
        }
        GameActionEntity action = new GameActionEntity();
        action.setRoomId(roomId);
        action.setRoundNumber(round);
        action.setPhase(phase);
        action.setActorPlayerId(actorId);
        action.setActionType(type);
        action.setTargetPlayerId(targetId);
        action.setSecondaryTargetPlayerId(secondaryTargetId);
        action.setScope(MemoryScope.PRIVATE);
        action.setResolved(resolved);
        actionRepository.save(action);
    }

    private GamePhase phaseFor(ActionType type) {
        return switch (type) {
            case KILL -> GamePhase.WEREWOLF_ACTION;
            case CHECK -> GamePhase.SEER_ACTION;
            case SAVE, POISON -> GamePhase.WITCH_ACTION;
            case GUARD -> GamePhase.GUARD_ACTION;
            case SWAP, LINK_LOVERS -> GamePhase.OTHER_NIGHT_ACTION;
            default -> GamePhase.NIGHT;
        };
    }

    private Optional<String> targetId(GameActionEntity action) {
        return Optional.ofNullable(action.getTargetPlayerId()).filter(target -> !target.isBlank());
    }

    private Optional<PlayerEntity> findPlayer(String playerId) {
        return playerRepository.findById(Objects.requireNonNull(playerId, "playerId must not be null"));
    }

    private String actorId(GameActionEntity action) {
        String actorId = action.getActorPlayerId();
        if (actorId == null || actorId.isBlank()) {
            throw new BusinessException("INVALID_ACTION", "行动记录缺少行动玩家");
        }
        return actorId;
    }

    private boolean requiresPrimaryTarget(ActionType type) {
        return switch (type) {
            case KILL, CHECK, POISON, GUARD, SHOOT, DUEL, EXPLODE_AND_KILL -> true;
            default -> false;
        };
    }

    private boolean blank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
