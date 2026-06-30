package com.example.aiwerewolf.speech.service;

import com.example.aiwerewolf.agent.core.AiAgentService;
import com.example.aiwerewolf.agent.dto.AiSpeechDecision;
import com.example.aiwerewolf.aiinfra.run.AgentRunPurpose;
import com.example.aiwerewolf.aiinfra.worker.AgentTaskRequest;
import com.example.aiwerewolf.aiinfra.worker.AgentTaskService;
import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.game.engine.GameOperationValidator;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.view.GameViewBuilder;
import com.example.aiwerewolf.memory.service.MemoryService;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.entity.PlayerType;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.speech.entity.SpeechEntity;
import com.example.aiwerewolf.speech.repository.SpeechRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SpeechService {
    private final SpeechRepository speechRepository;
    private final PlayerRepository playerRepository;
    private final AiAgentService aiAgentService;
    private final GameViewBuilder gameViewBuilder;
    private final MemoryService memoryService;
    private final GameOperationValidator operationValidator;
    private final AgentTaskService agentTaskService;
    private final ObjectMapper objectMapper;

    public SpeechService(SpeechRepository speechRepository,
                         PlayerRepository playerRepository,
                         AiAgentService aiAgentService,
                         GameViewBuilder gameViewBuilder,
                         MemoryService memoryService,
                         GameOperationValidator operationValidator,
                         AgentTaskService agentTaskService,
                         ObjectMapper objectMapper) {
        this.speechRepository = speechRepository;
        this.playerRepository = playerRepository;
        this.aiAgentService = aiAgentService;
        this.gameViewBuilder = gameViewBuilder;
        this.memoryService = memoryService;
        this.operationValidator = operationValidator;
        this.agentTaskService = agentTaskService;
        this.objectMapper = objectMapper;
    }

    public SpeechEntity submitHumanSpeech(String roomId, int round, String playerId, SpeechRequest request) {
        operationValidator.requirePhase(roomId, GamePhase.DAY_SPEECH);
        PlayerEntity player = operationValidator.requirePlayerInRoom(roomId, playerId);
        if (!player.isAlive() || !player.isCanSpeak()) {
            throw new BusinessException("ILLEGAL_PHASE_OPERATION", "当前玩家不能发言");
        }
        return saveSpeech(roomId, round, playerId, request.content(), request.claimedRole());
    }

    public boolean processNextAiSpeech(String roomId, int round) {
        List<PlayerEntity> speakers = playerRepository.findByRoomIdAndAliveTrueOrderBySeatNumberAsc(roomId).stream()
                .filter(PlayerEntity::isCanSpeak)
                .toList();
        for (PlayerEntity player : speakers) {
            if (speechRepository.findByRoomIdAndRoundNumberAndPlayerId(roomId, round, player.getId()).isPresent()) {
                continue;
            }
            // 严格按座位等待真人或处理一个 AI，保证后置位读取到前序发言后才开始思考。
            if (player.getType() != PlayerType.AI) {
                return false;
            }
            memoryService.appendPublicMemory(roomId, round, GamePhase.DAY_SPEECH, "SPEECH_TURN_STARTED",
                    playerLabel(roomId, player.getId()) + " 正在思考发言",
                    speechTurnMetadata(player));
            AiSpeechDecision decision = agentTaskService.execute(
                    new AgentTaskRequest(roomId, player.getId(), round, GamePhase.DAY_SPEECH, AgentRunPurpose.SPEECH),
                    () -> aiAgentService.generateSpeech(player.getId(), gameViewBuilder.buildPrivateView(roomId, player.getId())));
            saveSpeech(roomId, round, player.getId(), decision.speech(), parseRole(decision.claimedRole()));
            appendPublicSpeechSummary(roomId, round);
            memoryService.appendPrivateMemory(roomId, round, GamePhase.DAY_SPEECH, player.getId(),
                    "AI_STRATEGY", decision.strategySummary());
            memoryService.appendPublicMemory(roomId, round, GamePhase.DAY_SPEECH, "SPEECH_TURN_COMPLETED",
                    playerLabel(roomId, player.getId()) + " 已完成发言",
                    speechTurnMetadata(player));
            return speechesComplete(roomId, round, speakers);
        }
        return true;
    }

    public List<SpeechEntity> listPublicSpeeches(String roomId) {
        return speechRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream().filter(SpeechEntity::isPublicVisible).toList();
    }

    private SpeechEntity saveSpeech(String roomId, int round, String playerId, String content, @Nullable Role claimedRole) {
        return speechRepository.findByRoomIdAndRoundNumberAndPlayerId(roomId, round, playerId)
                .orElseGet(() -> {
                    SpeechEntity speech = new SpeechEntity();
                    speech.setRoomId(roomId);
                    speech.setRoundNumber(round);
                    speech.setPlayerId(playerId);
                    speech.setContent(content);
                    speech.setClaimedRole(claimedRole);
                    speech.setPublicVisible(true);
                    // flush 后再构建下一位 Agent 的视角，避免同一推进事务内看不到刚完成的发言。
                    SpeechEntity saved = speechRepository.saveAndFlush(speech);
                    memoryService.appendPublicMemory(roomId, round, GamePhase.DAY_SPEECH, "SPEECH",
                            playerLabel(roomId, playerId) + " 发言：" + content);
                    return saved;
                });
    }

    private boolean speechesComplete(String roomId, int round, List<PlayerEntity> speakers) {
        return speakers.stream().allMatch(player -> speechRepository
                .findByRoomIdAndRoundNumberAndPlayerId(roomId, round, player.getId())
                .isPresent());
    }

    private String playerLabel(String roomId, String playerId) {
        return playerRepository.findById(playerId)
                .filter(player -> roomId.equals(player.getRoomId()))
                .map(player -> player.getSeatNumber() + " 号 " + player.getName())
                .orElse(playerId);
    }

    @Nullable
    private Role parseRole(@Nullable String claimedRole) {
        if (claimedRole == null || claimedRole.isBlank() || "好人".equals(claimedRole)) {
            return null;
        }
        for (Role role : Role.values()) {
            if (role.name().equalsIgnoreCase(claimedRole) || role.displayName().equals(claimedRole)) {
                return role;
            }
        }
        return null;
    }

    private void appendPublicSpeechSummary(String roomId, int round) {
        List<SpeechEntity> roundSpeeches = speechRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .filter(SpeechEntity::isPublicVisible)
                .filter(speech -> speech.getRoundNumber() == round)
                .toList();
        if (roundSpeeches.isEmpty()) {
            return;
        }
        int fromIndex = Math.max(0, roundSpeeches.size() - 5);
        String summary = roundSpeeches.subList(fromIndex, roundSpeeches.size()).stream()
                .map(speech -> playerLabel(roomId, speech.getPlayerId()) + "：" + speech.getContent())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        memoryService.appendPublicMemory(roomId, round, GamePhase.DAY_SPEECH, "PUBLIC_SPEECH_SUMMARY",
                "当前公开发言摘要：\n" + summary,
                speechSummaryMetadata(roomId, round, roundSpeeches));
    }

    private String speechTurnMetadata(PlayerEntity player) {
        return toJson(Map.of(
                "playerId", player.getId(),
                "seatNumber", player.getSeatNumber(),
                "playerName", player.getName()));
    }

    private String speechSummaryMetadata(String roomId, int round, List<SpeechEntity> roundSpeeches) {
        // 摘要不仅保留文本，也保留结构化发言列表，GodView 后续可以按玩家、轮次做评估。
        List<Map<String, Object>> speeches = roundSpeeches.stream()
                .map(speech -> Map.<String, Object>of(
                        "playerId", speech.getPlayerId(),
                        "playerLabel", playerLabel(roomId, speech.getPlayerId()),
                        "roundNumber", speech.getRoundNumber(),
                        "content", speech.getContent(),
                        "claimedRole", speech.getClaimedRole() == null ? "" : speech.getClaimedRole().name()))
                .toList();
        return toJson(Map.of(
                "roundNumber", round,
                "speechCount", roundSpeeches.size(),
                "speeches", speeches));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

}
