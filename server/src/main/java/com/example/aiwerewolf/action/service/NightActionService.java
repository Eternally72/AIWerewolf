package com.example.aiwerewolf.action.service;

import com.example.aiwerewolf.action.entity.ActionType;
import com.example.aiwerewolf.action.entity.GameActionEntity;
import com.example.aiwerewolf.action.repository.GameActionRepository;
import com.example.aiwerewolf.agent.core.AiAgentService;
import com.example.aiwerewolf.agent.dto.AiActionDecision;
import com.example.aiwerewolf.aiinfra.run.AgentRunPurpose;
import com.example.aiwerewolf.aiinfra.worker.AgentTaskRequest;
import com.example.aiwerewolf.aiinfra.worker.AgentTaskService;
import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.game.engine.GameOperationValidator;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.runtime.IdempotencyService;
import com.example.aiwerewolf.game.view.GameViewBuilder;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import com.example.aiwerewolf.memory.service.MemoryService;
import com.example.aiwerewolf.player.entity.DeathReason;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.entity.PlayerType;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.role.ability.RoleAbility;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.role.service.RoleAbilityRegistry;
import com.example.aiwerewolf.game.view.GameView;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.time.Duration;

@Service
public class NightActionService {
    private final PlayerRepository playerRepository;
    private final GameActionRepository actionRepository;
    private final GameViewBuilder gameViewBuilder;
    private final AiAgentService aiAgentService;
    private final DeathResolutionService deathResolutionService;
    private final MemoryService memoryService;
    private final RoleAbilityRegistry roleAbilityRegistry;
    private final IdempotencyService idempotencyService;
    private final GameOperationValidator operationValidator;
    private final AgentTaskService agentTaskService;

    public NightActionService(PlayerRepository playerRepository,
                              GameActionRepository actionRepository,
                              GameViewBuilder gameViewBuilder,
                              AiAgentService aiAgentService,
                              DeathResolutionService deathResolutionService,
                              MemoryService memoryService,
                              RoleAbilityRegistry roleAbilityRegistry,
                              IdempotencyService idempotencyService,
                              GameOperationValidator operationValidator,
                              AgentTaskService agentTaskService) {
        this.playerRepository = playerRepository;
        this.actionRepository = actionRepository;
        this.gameViewBuilder = gameViewBuilder;
        this.aiAgentService = aiAgentService;
        this.deathResolutionService = deathResolutionService;
        this.memoryService = memoryService;
        this.roleAbilityRegistry = roleAbilityRegistry;
        this.idempotencyService = idempotencyService;
        this.operationValidator = operationValidator;
        this.agentTaskService = agentTaskService;
    }

    public boolean processNextAiNightAction(String roomId, int round, GamePhase phase) {
        List<PlayerEntity> actors = playerRepository.findByRoomIdAndAliveTrueOrderBySeatNumberAsc(roomId).stream()
                .filter(player -> canActInNightPhase(player, phase))
                .toList();
        for (PlayerEntity player : actors) {
            if (actionRepository.existsByRoomIdAndRoundNumberAndPhaseAndActorPlayerId(
                    roomId, round, phase, player.getId())) {
                continue;
            }
            // 夜间同样按座位逐个行动；真人未提交时必须停在当前玩家，不能越过并调用后续 Agent。
            if (player.getType() != PlayerType.AI) {
                return false;
            }
            RoleAbility ability = roleAbilityRegistry.get(player.getRole());
            GameView privateView = gameViewBuilder.buildPrivateView(roomId, player.getId());
            AiActionDecision decision = agentTaskService.execute(
                    new AgentTaskRequest(roomId, player.getId(), round, phase, AgentRunPurpose.NIGHT_ACTION),
                    () -> ability.canAct(privateView, player)
                            ? aiAgentService.decideNightAction(player.getId(), privateView)
                            : noneDecision("当前角色无需行动"));
            if (!validDecision(roomId, round, player, ability, privateView, decision)) {
                decision = noneDecision("模型行动无效，系统记录为无行动");
            }
            GamePhase actionPhase = decision.actionType() == ActionType.NONE ? phase : phaseFor(decision.actionType());
            saveAction(roomId, round, actionPhase, player.getId(), decision.actionType(),
                    decision.targetPlayerId(), decision.secondaryTargetPlayerId(), false);
            // 公共视角只获知夜间步骤已完成，不暴露行动者、目标和具体技能。
            memoryService.appendPublicMemory(roomId, round, phase, "NIGHT_TURN_COMPLETED",
                    "一名夜间角色已完成行动");
            return nightActionsComplete(roomId, round, phase, actors);
        }
        return true;
    }

    public void submitHumanNightAction(String roomId, int round, String playerId, GameActionRequest request) {
        operationValidator.requireNightActionPhase(roomId, request.actionType());
        PlayerEntity actor = operationValidator.requirePlayerInRoom(roomId, playerId);
        if (!actor.isAlive()) {
            throw new BusinessException("ILLEGAL_PHASE_OPERATION", "死亡玩家不能行动");
        }
        Role role = actor.getRole();
        if (role == null) {
            throw new BusinessException("ILLEGAL_PHASE_OPERATION", "玩家尚未分配身份");
        }
        GamePhase currentPhase = operationValidator.requireRoom(roomId).getPhase();
        if (request.actionType() == ActionType.NONE) {
            saveAction(roomId, round, currentPhase, playerId, ActionType.NONE, null, null, false);
            return;
        }
        RoleAbility ability = roleAbilityRegistry.get(role);
        if (requiresPrimaryTarget(request.actionType()) && blank(request.targetPlayerId())) {
            throw new BusinessException("ILLEGAL_TARGET", "该行动必须选择目标玩家");
        }
        if (!ability.validateAction(gameViewBuilder.buildPrivateView(roomId, playerId), toAction(roomId, round, playerId, request))) {
            throw new BusinessException("ILLEGAL_TARGET", "该行动不符合角色能力规则");
        }
        if (request.targetPlayerId() != null && !request.targetPlayerId().isBlank()) {
            operationValidator.requireAliveTargetInRoom(roomId, request.targetPlayerId());
        }
        if (request.secondaryTargetPlayerId() != null && !request.secondaryTargetPlayerId().isBlank()) {
            operationValidator.requireAliveTargetInRoom(roomId, request.secondaryTargetPlayerId());
        }
        saveAction(roomId, round, phaseFor(request.actionType()), playerId, request.actionType(), request.targetPlayerId(), request.secondaryTargetPlayerId(), false);
    }

    public void resolveNightActions(String roomId, int round) {
        if (!idempotencyService.markIfAbsent("idempotency:%s:%s:night-resolution".formatted(roomId, round), Duration.ofHours(12))) {
            return;
        }
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
        String key = "idempotency:%s:%s:%s:%s:%s".formatted(roomId, round, phase, actorId, type);
        if (!idempotencyService.markIfAbsent(key, Duration.ofHours(12))) {
            return;
        }
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
        appendSharedActionMemory(roomId, round, phase, actorId, type, targetId);
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

    private GameActionEntity toAction(String roomId, int round, String actorId, AiActionDecision decision) {
        GameActionEntity action = new GameActionEntity();
        action.setRoomId(roomId);
        action.setRoundNumber(round);
        action.setPhase(phaseFor(decision.actionType()));
        action.setActorPlayerId(actorId);
        action.setActionType(decision.actionType());
        action.setTargetPlayerId(decision.targetPlayerId());
        action.setSecondaryTargetPlayerId(decision.secondaryTargetPlayerId());
        action.setScope(MemoryScope.PRIVATE);
        return action;
    }

    private GameActionEntity toAction(String roomId, int round, String actorId, GameActionRequest request) {
        GameActionEntity action = new GameActionEntity();
        action.setRoomId(roomId);
        action.setRoundNumber(round);
        action.setPhase(phaseFor(request.actionType()));
        action.setActorPlayerId(actorId);
        action.setActionType(request.actionType());
        action.setTargetPlayerId(request.targetPlayerId());
        action.setSecondaryTargetPlayerId(request.secondaryTargetPlayerId());
        action.setScope(MemoryScope.PRIVATE);
        return action;
    }

    private void appendSharedActionMemory(String roomId, int round, GamePhase phase, String actorId, ActionType type, @Nullable String targetId) {
        if (type != ActionType.KILL) {
            return;
        }
        List<String> wolves = playerRepository.findByRoomIdOrderBySeatNumberAsc(roomId).stream()
                .filter(player -> player.getRole() != null && player.getRole().isWerewolfCamp())
                .map(PlayerEntity::getId)
                .toList();
        if (!wolves.isEmpty()) {
            String actorName = playerLabel(roomId, actorId);
            String targetName = targetId == null || targetId.isBlank() ? "未知目标" : playerLabel(roomId, targetId);
            memoryService.appendSharedSecretMemory(roomId, round, phase, MemoryScope.WEREWOLF_TEAM, wolves,
                    "WEREWOLF_TEAM_ACTION", actorName + " 提议夜间击杀 " + targetName);
        }
    }

    private String playerLabel(String roomId, String playerId) {
        return playerRepository.findById(playerId)
                .filter(player -> roomId.equals(player.getRoomId()))
                .map(player -> player.getSeatNumber() + " 号 " + player.getName())
                .orElse(playerId);
    }

    private boolean canActInNightPhase(PlayerEntity player, GamePhase phase) {
        Role role = player.getRole();
        if (role == null) {
            return false;
        }
        RoleAbility ability = roleAbilityRegistry.get(role);
        return ability.getNightActionType() != ActionType.NONE && phaseFor(ability.getNightActionType()) == phase;
    }

    private boolean validDecision(String roomId,
                                  int round,
                                  PlayerEntity player,
                                  RoleAbility ability,
                                  GameView privateView,
                                  AiActionDecision decision) {
        if (decision.actionType() == ActionType.NONE) {
            return true;
        }
        if (phaseFor(decision.actionType()) != privateView.phase()) {
            return false;
        }
        if (requiresPrimaryTarget(decision.actionType()) && blank(decision.targetPlayerId())) {
            return false;
        }
        return ability.validateAction(privateView, toAction(roomId, round, player.getId(), decision));
    }

    private boolean nightActionsComplete(String roomId, int round, GamePhase phase, List<PlayerEntity> actors) {
        return actors.stream().allMatch(player -> actionRepository
                .existsByRoomIdAndRoundNumberAndPhaseAndActorPlayerId(roomId, round, phase, player.getId()));
    }

    private AiActionDecision noneDecision(String reason) {
        return new AiActionDecision(ActionType.NONE, null, null, reason);
    }
}
