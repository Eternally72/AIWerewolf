package com.example.aiwerewolf.vote.service;

import com.example.aiwerewolf.agent.core.AiAgentService;
import com.example.aiwerewolf.agent.dto.AiVoteDecision;
import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.view.GameViewBuilder;
import com.example.aiwerewolf.memory.service.MemoryService;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.entity.PlayerType;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.vote.entity.VoteEntity;
import com.example.aiwerewolf.vote.repository.VoteRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VoteService {
    private final VoteRepository voteRepository;
    private final PlayerRepository playerRepository;
    private final AiAgentService aiAgentService;
    private final GameViewBuilder gameViewBuilder;
    private final MemoryService memoryService;

    public VoteService(VoteRepository voteRepository,
                       PlayerRepository playerRepository,
                       AiAgentService aiAgentService,
                       GameViewBuilder gameViewBuilder,
                       MemoryService memoryService) {
        this.voteRepository = voteRepository;
        this.playerRepository = playerRepository;
        this.aiAgentService = aiAgentService;
        this.gameViewBuilder = gameViewBuilder;
        this.memoryService = memoryService;
    }

    public VoteEntity submitHumanVote(String roomId, int round, String playerId, VoteRequest request) {
        PlayerEntity voter = playerRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_FOUND", "玩家不存在"));
        if (!voter.isAlive() || !voter.isCanVote()) {
            throw new BusinessException("ILLEGAL_PHASE_OPERATION", "当前玩家不能投票");
        }
        playerRepository.findById(request.targetPlayerId())
                .filter(PlayerEntity::isAlive)
                .orElseThrow(() -> new BusinessException("ILLEGAL_TARGET", "投票目标非法"));
        return saveVote(roomId, round, playerId, request.targetPlayerId(), request.reason());
    }

    public void generateAiVotes(String roomId, int round) {
        for (PlayerEntity player : playerRepository.findByRoomIdAndAliveTrueOrderBySeatNumberAsc(roomId)) {
            if (player.getType() != PlayerType.AI || !player.isCanVote()) {
                continue;
            }
            AiVoteDecision decision = aiAgentService.decideVote(player.getId(), gameViewBuilder.buildPrivateView(roomId, player.getId()));
            if (decision.targetPlayerId() != null) {
                saveVote(roomId, round, player.getId(), decision.targetPlayerId(), decision.reason());
            }
        }
    }

    public Optional<String> calculateVoteResult(String roomId, int round) {
        Map<String, Long> counts = voteRepository.findByRoomIdAndRoundNumber(roomId, round).stream()
                .map(VoteEntity::getTargetPlayerId)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        if (counts.isEmpty()) {
            return Optional.empty();
        }
        long top = counts.values().stream().max(Long::compareTo).orElse(0L);
        List<String> leaders = counts.entrySet().stream()
                .filter(e -> e.getValue() == top)
                .map(Map.Entry::getKey)
                .sorted(Comparator.naturalOrder())
                .toList();
        return leaders.size() == 1 ? Optional.of(leaders.getFirst()) : Optional.empty();
    }

    public VoteEntity saveVote(String roomId, int round, String voterId, String targetId, String reason) {
        return voteRepository.findByRoomIdAndRoundNumberAndVoterPlayerId(roomId, round, voterId)
                .orElseGet(() -> {
                    VoteEntity vote = new VoteEntity();
                    vote.setRoomId(roomId);
                    vote.setRoundNumber(round);
                    vote.setVoterPlayerId(voterId);
                    vote.setTargetPlayerId(targetId);
                    vote.setReason(reason == null ? "无" : reason);
                    VoteEntity saved = voteRepository.save(vote);
                    memoryService.appendPublicMemory(roomId, round, GamePhase.DAY_VOTE, "VOTE",
                            voterId + " 投票给 " + targetId);
                    return saved;
                });
    }
}
