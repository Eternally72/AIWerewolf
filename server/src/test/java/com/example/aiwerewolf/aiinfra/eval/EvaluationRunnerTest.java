package com.example.aiwerewolf.aiinfra.eval;

import com.example.aiwerewolf.aiinfra.observability.AiInfraMetrics;
import com.example.aiwerewolf.aiinfra.run.AgentRunEntity;
import com.example.aiwerewolf.aiinfra.run.AgentRunPurpose;
import com.example.aiwerewolf.aiinfra.run.AgentRunRepository;
import com.example.aiwerewolf.aiinfra.run.AgentRunStatus;
import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.game.engine.GamePhaseEngine;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.game.view.GameViewBuilder;
import com.example.aiwerewolf.game.view.PlayerView;
import com.example.aiwerewolf.player.entity.PlayerType;
import com.example.aiwerewolf.room.dto.RoomResponse;
import com.example.aiwerewolf.room.entity.HumanMode;
import com.example.aiwerewolf.room.entity.ObserverViewMode;
import com.example.aiwerewolf.room.entity.RoomEntity;
import com.example.aiwerewolf.room.entity.RoomStatus;
import com.example.aiwerewolf.room.service.RoomService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvaluationRunnerTest {
    private final RoomService roomService = mock(RoomService.class);
    private final GamePhaseEngine gamePhaseEngine = mock(GamePhaseEngine.class);
    private final GameViewBuilder gameViewBuilder = mock(GameViewBuilder.class);
    private final AgentRunRepository agentRunRepository = mock(AgentRunRepository.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final EvaluationRunner runner = new EvaluationRunner(
            roomService,
            gamePhaseEngine,
            gameViewBuilder,
            agentRunRepository,
            new AiInfraMetrics(meterRegistry));

    @Test
    void runsSevenPlayerEvaluationAndCollectsMetrics() {
        String roomId = "room-1";
        when(roomService.createRoom(any())).thenReturn(roomResponse(roomId));
        when(gamePhaseEngine.advanceUntilGameOver(roomId)).thenReturn(gameOverRoom(roomId));
        when(agentRunRepository.findByRoomIdOrderByCreatedAtAsc(roomId)).thenReturn(List.of(
                agentRun(roomId, false, null, 10),
                agentRun(roomId, true, "模型投票输出为空、格式非法或目标非法，已使用 fallback 投票", 20)
        ));
        when(gameViewBuilder.buildPublicView(roomId)).thenReturn(publicView(roomId));

        EvaluationReport report = runner.run(new EvaluationRunRequest(1, "7-standard"));

        assertThat(report.requestedGames()).isEqualTo(1);
        assertThat(report.completedGames()).isEqualTo(1);
        assertThat(report.completionRate()).isEqualTo(1.0);
        assertThat(report.totalAgentRuns()).isEqualTo(2);
        assertThat(report.totalFallbackRuns()).isEqualTo(1);
        assertThat(report.invalidDecisionFallbackCount()).isEqualTo(1);
        assertThat(report.leakageCount()).isZero();
        assertThat(report.averageRounds()).isEqualTo(3.0);
        assertThat(meterRegistry.counter("aiwerewolf.evaluation.runs", "template", "7-standard").count()).isEqualTo(1.0);
    }

    @Test
    void rejectsUnsupportedTemplate() {
        assertThatThrownBy(() -> runner.run(new EvaluationRunRequest(1, "12-advanced")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前评测仅支持 7-standard 模板");
    }

    private RoomResponse roomResponse(String roomId) {
        Instant now = Instant.now();
        return new RoomResponse(roomId, "eval", RoomStatus.WAITING, GamePhase.WAITING, 7,
                HumanMode.NONE, ObserverViewMode.GOD_VIEW, now, now, "token");
    }

    private RoomEntity gameOverRoom(String roomId) {
        RoomEntity room = new RoomEntity();
        room.setId(roomId);
        room.setName("eval");
        room.setStatus(RoomStatus.GAME_OVER);
        room.setPhase(GamePhase.GAME_OVER);
        room.setCurrentRound(3);
        room.setTotalSeats(7);
        room.setHumanMode(HumanMode.NONE);
        room.setObserverViewMode(ObserverViewMode.GOD_VIEW);
        return room;
    }

    private GameView publicView(String roomId) {
        return new GameView(roomId, "eval", RoomStatus.GAME_OVER, GamePhase.GAME_OVER, 3,
                null, null, null,
                List.of(new PlayerView("p1", 1, "P1", PlayerType.AI, true, true, true, null, null)),
                List.of(), List.of(), List.of(), false);
    }

    private AgentRunEntity agentRun(String roomId, boolean fallbackUsed, String errorMessage, long latencyMillis) {
        AgentRunEntity run = new AgentRunEntity();
        run.setRoomId(roomId);
        run.setPlayerId("p1");
        run.setAgentId("p1");
        run.setRoundNumber(1);
        run.setPhase(GamePhase.DAY_VOTE);
        run.setPurpose(AgentRunPurpose.VOTE);
        run.setStatus(fallbackUsed ? AgentRunStatus.FALLBACK : AgentRunStatus.SUCCESS);
        run.setFallbackUsed(fallbackUsed);
        run.setAttemptCount(1);
        run.setLatencyMillis(latencyMillis);
        run.setPromptVersion("role-prompts-v1:VILLAGER");
        run.setTaskPromptVersion("task-prompts-v1:VOTE/output-schema-v1:VOTE");
        run.setModelProvider("mock");
        run.setModelName("mock-json-v1");
        run.setInputViewSnapshotJson("{}");
        run.setErrorMessage(errorMessage);
        return run;
    }
}
