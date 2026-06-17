package com.example.aiwerewolf.aiinfra.eval;

import com.example.aiwerewolf.aiinfra.observability.AiInfraMetrics;
import com.example.aiwerewolf.aiinfra.run.AgentRunEntity;
import com.example.aiwerewolf.aiinfra.run.AgentRunRepository;
import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.game.engine.GamePhaseEngine;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.game.view.GameViewBuilder;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import com.example.aiwerewolf.room.dto.CreateRoomRequest;
import com.example.aiwerewolf.room.dto.RoomResponse;
import com.example.aiwerewolf.room.entity.RoomEntity;
import com.example.aiwerewolf.room.service.RoomService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EvaluationRunner {
    private final RoomService roomService;
    private final GamePhaseEngine gamePhaseEngine;
    private final GameViewBuilder gameViewBuilder;
    private final AgentRunRepository agentRunRepository;
    private final AiInfraMetrics metrics;

    public EvaluationRunner(RoomService roomService,
                            GamePhaseEngine gamePhaseEngine,
                            GameViewBuilder gameViewBuilder,
                            AgentRunRepository agentRunRepository,
                            AiInfraMetrics metrics) {
        this.roomService = roomService;
        this.gamePhaseEngine = gamePhaseEngine;
        this.gameViewBuilder = gameViewBuilder;
        this.agentRunRepository = agentRunRepository;
        this.metrics = metrics;
    }

    public EvaluationReport run(EvaluationRunRequest request) {
        String templateId = request.safeTemplateId();
        if (!"7-standard".equals(templateId)) {
            throw new BusinessException("UNSUPPORTED_EVALUATION_TEMPLATE", "当前评测仅支持 7-standard 模板");
        }
        int gameCount = Math.min(20, request.safeGameCount());
        long suiteStartedAt = System.nanoTime();
        List<EvaluationGameResult> games = new ArrayList<>();
        for (int index = 1; index <= gameCount; index++) {
            games.add(runSingleGame(index));
        }
        return buildReport(UUID.randomUUID().toString(), templateId, gameCount, suiteStartedAt, games);
    }

    private EvaluationGameResult runSingleGame(int index) {
        long startedAt = System.nanoTime();
        String roomId = null;
        try {
            RoomResponse created = roomService.createRoom(evaluationRoomRequest(index));
            roomId = created.id();
            roomService.startGame(roomId);
            RoomEntity finalRoom = gamePhaseEngine.advanceUntilGameOver(roomId);
            List<AgentRunEntity> runs = agentRunRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
            int leakageCount = detectPublicLeakage(roomId);
            return new EvaluationGameResult(
                    index,
                    roomId,
                    finalRoom.getPhase() == GamePhase.GAME_OVER,
                    finalRoom.getStatus(),
                    finalRoom.getPhase(),
                    finalRoom.getCurrentRound(),
                    elapsedMillis(startedAt),
                    runs.size(),
                    fallbackCount(runs),
                    invalidDecisionFallbackCount(runs),
                    leakageCount,
                    null);
        } catch (Exception ex) {
            List<AgentRunEntity> runs = roomId == null ? List.of() : agentRunRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
            return new EvaluationGameResult(
                    index,
                    roomId,
                    false,
                    null,
                    null,
                    0,
                    elapsedMillis(startedAt),
                    runs.size(),
                    fallbackCount(runs),
                    invalidDecisionFallbackCount(runs),
                    roomId == null ? 0 : detectPublicLeakageSafely(roomId),
                    safeError(ex));
        }
    }

    private CreateRoomRequest evaluationRoomRequest(int index) {
        CreateRoomRequest request = CreateRoomRequest.defaultSevenAi();
        return new CreateRoomRequest(
                "Eval 7人标准局 #" + index,
                request.totalSeats(),
                request.humanMode(),
                request.humanPlayerName(),
                request.humanRoleAssignMode(),
                request.specifiedHumanRole(),
                request.observerViewMode(),
                request.roleConfig(),
                request.ruleConfig(),
                request.uiConfig());
    }

    private EvaluationReport buildReport(String evaluationId,
                                         String templateId,
                                         int requestedGames,
                                         long suiteStartedAt,
                                         List<EvaluationGameResult> games) {
        int completed = (int) games.stream().filter(EvaluationGameResult::completed).count();
        int totalRuns = games.stream().mapToInt(EvaluationGameResult::agentRunCount).sum();
        int fallbackRuns = games.stream().mapToInt(EvaluationGameResult::fallbackRunCount).sum();
        int invalidFallbacks = games.stream().mapToInt(EvaluationGameResult::invalidDecisionFallbackCount).sum();
        int leakageCount = games.stream().mapToInt(EvaluationGameResult::leakageCount).sum();
        double averageLatency = games.stream()
                .filter(game -> game.agentRunCount() > 0)
                .mapToDouble(this::averageRunLatency)
                .average()
                .orElse(0.0);
        double averageRounds = games.stream()
                .filter(EvaluationGameResult::completed)
                .mapToInt(EvaluationGameResult::roundNumber)
                .average()
                .orElse(0.0);
        EvaluationReport report = new EvaluationReport(
                evaluationId,
                templateId,
                requestedGames,
                completed,
                requestedGames - completed,
                requestedGames == 0 ? 0.0 : (double) completed / requestedGames,
                totalRuns,
                fallbackRuns,
                totalRuns == 0 ? 0.0 : (double) fallbackRuns / totalRuns,
                invalidFallbacks,
                leakageCount,
                averageLatency,
                averageRounds,
                elapsedMillis(suiteStartedAt),
                Instant.now(),
                games);
        metrics.recordEvaluation(
                templateId,
                report.completedGames(),
                report.failedGames(),
                report.leakageCount(),
                report.fallbackRate(),
                report.totalDurationMillis());
        return report;
    }

    private double averageRunLatency(EvaluationGameResult game) {
        if (game.roomId() == null) {
            return 0.0;
        }
        return agentRunRepository.findByRoomIdOrderByCreatedAtAsc(game.roomId()).stream()
                .mapToLong(AgentRunEntity::getLatencyMillis)
                .average()
                .orElse(0.0);
    }

    private int detectPublicLeakage(String roomId) {
        GameView publicView = gameViewBuilder.buildPublicView(roomId);
        int playerLeaks = (int) publicView.players().stream()
                .filter(player -> player.role() != null || player.camp() != null)
                .count();
        int memoryLeaks = (int) publicView.memories().stream()
                .filter(memory -> memory.scope() != MemoryScope.PUBLIC)
                .count();
        return playerLeaks + memoryLeaks;
    }

    private int detectPublicLeakageSafely(String roomId) {
        try {
            return detectPublicLeakage(roomId);
        } catch (Exception ex) {
            return 0;
        }
    }

    private int fallbackCount(List<AgentRunEntity> runs) {
        return (int) runs.stream().filter(AgentRunEntity::isFallbackUsed).count();
    }

    private int invalidDecisionFallbackCount(List<AgentRunEntity> runs) {
        return (int) runs.stream()
                .filter(AgentRunEntity::isFallbackUsed)
                .filter(run -> contains(run.getErrorMessage(), "非法"))
                .count();
    }

    private boolean contains(@Nullable String text, String pattern) {
        return text != null && text.contains(pattern);
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private String safeError(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }
}
