package com.example.aiwerewolf.vote.service;

import com.example.aiwerewolf.agent.core.AiAgentService;
import com.example.aiwerewolf.agent.dto.AiVoteDecision;
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
import com.example.aiwerewolf.vote.entity.VoteEntity;
import com.example.aiwerewolf.vote.repository.VoteRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
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
    private final GameOperationValidator operationValidator;
    private final AgentTaskService agentTaskService;
    private final ObjectMapper objectMapper;

    public VoteService(VoteRepository voteRepository,
                       PlayerRepository playerRepository,
                       AiAgentService aiAgentService,
                       GameViewBuilder gameViewBuilder,
                       MemoryService memoryService,
                       GameOperationValidator operationValidator,
                       AgentTaskService agentTaskService,
                       ObjectMapper objectMapper) {
        this.voteRepository = voteRepository;
        this.playerRepository = playerRepository;
        this.aiAgentService = aiAgentService;
        this.gameViewBuilder = gameViewBuilder;
        this.memoryService = memoryService;
        this.operationValidator = operationValidator;
        this.agentTaskService = agentTaskService;
        this.objectMapper = objectMapper;
    }

    public VoteEntity submitHumanVote(String roomId, int round, String playerId, VoteRequest request) {
        operationValidator.requirePhase(roomId, GamePhase.DAY_VOTE);
        PlayerEntity voter = operationValidator.requirePlayerInRoom(roomId, playerId);
        if (!voter.isAlive() || !voter.isCanVote()) {
            throw new BusinessException("ILLEGAL_PHASE_OPERATION", "当前玩家不能投票");
        }
        if (playerId.equals(request.targetPlayerId())) {
            throw new BusinessException("ILLEGAL_TARGET", "不能投票给自己");
        }
        operationValidator.requireAliveTargetInRoom(roomId, request.targetPlayerId());
        return saveVote(roomId, round, playerId, request.targetPlayerId(), request.reason());
    }

    public boolean processNextAiVote(String roomId, int round) {
        List<PlayerEntity> voters = playerRepository.findByRoomIdAndAliveTrueOrderBySeatNumberAsc(roomId).stream()
                .filter(PlayerEntity::isCanVote)
                .toList();
        for (PlayerEntity player : voters) {
            if (voteRepository.findByRoomIdAndRoundNumberAndVoterPlayerId(roomId, round, player.getId()).isPresent()) {
                continue;
            }
            // 投票顺序与发言顺序一致，每次推进只允许一个 Agent 做出私密决策。
            if (player.getType() != PlayerType.AI) {
                return false;
            }
            AiVoteDecision decision = agentTaskService.execute(
                    new AgentTaskRequest(roomId, player.getId(), round, GamePhase.DAY_VOTE, AgentRunPurpose.VOTE),
                    () -> aiAgentService.decideVote(player.getId(), gameViewBuilder.buildPrivateView(roomId, player.getId())));
            if (decision.targetPlayerId() != null) {
                saveVote(roomId, round, player.getId(), decision.targetPlayerId(), decision.reason());
            }
            memoryService.appendPublicMemory(roomId, round, GamePhase.DAY_VOTE, "VOTE_TURN_COMPLETED",
                    playerLabel(roomId, player.getId()) + " 已完成投票，目标将在结算时公开");
            return votesComplete(roomId, round, voters);
        }
        return true;
    }

    private boolean votesComplete(String roomId, int round, List<PlayerEntity> voters) {
        return voters.stream().allMatch(player -> voteRepository
                .findByRoomIdAndRoundNumberAndVoterPlayerId(roomId, round, player.getId())
                .isPresent());
    }

    public Optional<String> calculateVoteResult(String roomId, int round) {
        List<VoteEntity> votes = voteRepository.findByRoomIdAndRoundNumber(roomId, round);
        Map<String, Long> counts = votes.stream()
                .map(VoteEntity::getTargetPlayerId)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        if (counts.isEmpty()) {
            memoryService.appendPublicMemory(roomId, round, GamePhase.EXECUTION, "VOTE_RESULT",
                    "本轮无人投票，未放逐玩家",
                    toJson(Map.of("roundNumber", round, "votes", List.of(), "exiledPlayerId", "")));
            return Optional.empty();
        }
        long top = counts.values().stream().max(Long::compareTo).orElse(0L);
        List<String> leaders = counts.entrySet().stream()
                .filter(e -> e.getValue() == top)
                .map(Map.Entry::getKey)
                .sorted(Comparator.naturalOrder())
                .toList();
        Optional<String> result = leaders.size() == 1 ? Optional.of(leaders.getFirst()) : Optional.empty();
        memoryService.appendPublicMemory(roomId, round, GamePhase.EXECUTION, "VOTE_RESULT",
                publicVoteResult(roomId, votes, result),
                voteResultMetadata(roomId, round, votes, counts, result));
        return result;
    }

    public VoteEntity saveVote(String roomId, int round, String voterId, String targetId, @Nullable String reason) {
        return voteRepository.findByRoomIdAndRoundNumberAndVoterPlayerId(roomId, round, voterId)
                .orElseGet(() -> {
                    VoteEntity vote = new VoteEntity();
                    vote.setRoomId(roomId);
                    vote.setRoundNumber(round);
                    vote.setVoterPlayerId(voterId);
                    vote.setTargetPlayerId(targetId);
                    vote.setReason(reason == null ? "无" : reason);
                    VoteEntity saved = voteRepository.save(vote);
                    String content = playerLabel(roomId, voterId) + " 已私下投票给 " + playerLabel(roomId, targetId);
                    memoryService.appendPrivateMemory(roomId, round, GamePhase.DAY_VOTE, voterId, "PRIVATE_VOTE", content);
                    memoryService.appendGodViewMemory(roomId, round, GamePhase.DAY_VOTE, "PRIVATE_VOTE", content);
                    return saved;
                });
    }

    private String playerLabel(String roomId, String playerId) {
        return playerRepository.findById(playerId)
                .filter(player -> roomId.equals(player.getRoomId()))
                .map(player -> player.getSeatNumber() + " 号 " + player.getName())
                .orElse(playerId);
    }

    private String publicVoteResult(String roomId, List<VoteEntity> votes, Optional<String> exiledPlayerId) {
        String details = sortedVotes(roomId, votes).stream()
                .map(vote -> playerLabel(roomId, vote.getVoterPlayerId()) + " -> " + playerLabel(roomId, vote.getTargetPlayerId()))
                .collect(Collectors.joining("；\n"));
        String result = exiledPlayerId
                .map(playerId -> "放逐结果：" + playerLabel(roomId, playerId))
                .orElse("平票或无唯一最高票，未放逐玩家");
        return "投票图谱：\n" + details + "。\n" + result;
    }

    private String voteResultMetadata(String roomId, int round, List<VoteEntity> votes, Map<String, Long> counts, Optional<String> exiledPlayerId) {
        List<Map<String, Object>> voteGraph = sortedVotes(roomId, votes).stream()
                .map(vote -> Map.<String, Object>of(
                        "voterPlayerId", vote.getVoterPlayerId(),
                        "voterLabel", playerLabel(roomId, vote.getVoterPlayerId()),
                        "targetPlayerId", vote.getTargetPlayerId(),
                        "targetLabel", playerLabel(roomId, vote.getTargetPlayerId()),
                        "reason", vote.getReason() == null ? "" : vote.getReason()))
                .toList();
        Map<String, Long> sortedCounts = counts.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> seatNumber(roomId, entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
        return toJson(Map.of(
                "roundNumber", round,
                "votes", voteGraph,
                "counts", sortedCounts,
                "exiledPlayerId", exiledPlayerId.orElse("")));
    }

    private List<VoteEntity> sortedVotes(String roomId, List<VoteEntity> votes) {
        return votes.stream()
                .sorted(Comparator.comparingInt(vote -> seatNumber(roomId, vote.getVoterPlayerId())))
                .toList();
    }

    private int seatNumber(String roomId, String playerId) {
        return playerRepository.findById(playerId)
                .filter(player -> roomId.equals(player.getRoomId()))
                .map(PlayerEntity::getSeatNumber)
                .orElse(Integer.MAX_VALUE);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

}
