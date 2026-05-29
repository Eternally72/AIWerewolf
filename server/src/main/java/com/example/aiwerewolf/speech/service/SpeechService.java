package com.example.aiwerewolf.speech.service;

import com.example.aiwerewolf.agent.core.AiAgentService;
import com.example.aiwerewolf.agent.dto.AiSpeechDecision;
import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.view.GameViewBuilder;
import com.example.aiwerewolf.memory.service.MemoryService;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.entity.PlayerType;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.speech.entity.SpeechEntity;
import com.example.aiwerewolf.speech.repository.SpeechRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpeechService {
    private final SpeechRepository speechRepository;
    private final PlayerRepository playerRepository;
    private final AiAgentService aiAgentService;
    private final GameViewBuilder gameViewBuilder;
    private final MemoryService memoryService;

    public SpeechService(SpeechRepository speechRepository,
                         PlayerRepository playerRepository,
                         AiAgentService aiAgentService,
                         GameViewBuilder gameViewBuilder,
                         MemoryService memoryService) {
        this.speechRepository = speechRepository;
        this.playerRepository = playerRepository;
        this.aiAgentService = aiAgentService;
        this.gameViewBuilder = gameViewBuilder;
        this.memoryService = memoryService;
    }

    public SpeechEntity submitHumanSpeech(String roomId, int round, String playerId, SpeechRequest request) {
        PlayerEntity player = playerRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_FOUND", "玩家不存在"));
        if (!player.isAlive() || !player.isCanSpeak()) {
            throw new BusinessException("ILLEGAL_PHASE_OPERATION", "当前玩家不能发言");
        }
        return saveSpeech(roomId, round, playerId, request.content(), request.claimedRole());
    }

    public void generateAiSpeech(String roomId, int round) {
        for (PlayerEntity player : playerRepository.findByRoomIdAndAliveTrueOrderBySeatNumberAsc(roomId)) {
            if (player.getType() != PlayerType.AI) {
                continue;
            }
            AiSpeechDecision decision = aiAgentService.generateSpeech(player.getId(), gameViewBuilder.buildPrivateView(roomId, player.getId()));
            saveSpeech(roomId, round, player.getId(), decision.speech(), parseRole(decision.claimedRole()));
            memoryService.appendPrivateMemory(roomId, round, GamePhase.DAY_SPEECH, player.getId(),
                    "AI_STRATEGY", decision.strategySummary());
        }
    }

    public List<SpeechEntity> listPublicSpeeches(String roomId) {
        return speechRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream().filter(SpeechEntity::isPublicVisible).toList();
    }

    private SpeechEntity saveSpeech(String roomId, int round, String playerId, String content, Role claimedRole) {
        return speechRepository.findByRoomIdAndRoundNumberAndPlayerId(roomId, round, playerId)
                .orElseGet(() -> {
                    SpeechEntity speech = new SpeechEntity();
                    speech.setRoomId(roomId);
                    speech.setRoundNumber(round);
                    speech.setPlayerId(playerId);
                    speech.setContent(content);
                    speech.setClaimedRole(claimedRole);
                    speech.setPublicVisible(true);
                    SpeechEntity saved = speechRepository.save(speech);
                    memoryService.appendPublicMemory(roomId, round, GamePhase.DAY_SPEECH, "SPEECH", "玩家发言：" + content);
                    return saved;
                });
    }

    private Role parseRole(String claimedRole) {
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
}
