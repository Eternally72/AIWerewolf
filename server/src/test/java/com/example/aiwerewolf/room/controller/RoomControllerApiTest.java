package com.example.aiwerewolf.room.controller;

import com.example.aiwerewolf.game.engine.GamePhaseEngine;
import com.example.aiwerewolf.aiinfra.run.AgentRunPurpose;
import com.example.aiwerewolf.aiinfra.run.AgentRunResponse;
import com.example.aiwerewolf.aiinfra.run.AgentRunService;
import com.example.aiwerewolf.aiinfra.run.AgentRunStatus;
import com.example.aiwerewolf.aiinfra.worker.AgentTaskService;
import com.example.aiwerewolf.aiinfra.worker.AgentTaskSnapshot;
import com.example.aiwerewolf.aiinfra.worker.AgentTaskStatus;
import com.example.aiwerewolf.game.event.GameEventResponse;
import com.example.aiwerewolf.game.event.GameEventService;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.game.view.GameViewBuilder;
import com.example.aiwerewolf.game.view.PlayerView;
import com.example.aiwerewolf.player.entity.PlayerType;
import com.example.aiwerewolf.role.model.Camp;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.role.config.DefaultConfigService;
import com.example.aiwerewolf.role.config.RoleCatalogService;
import com.example.aiwerewolf.room.dto.RoomResponse;
import com.example.aiwerewolf.room.entity.HumanMode;
import com.example.aiwerewolf.room.entity.ObserverViewMode;
import com.example.aiwerewolf.room.entity.RoomEntity;
import com.example.aiwerewolf.room.entity.RoomStatus;
import com.example.aiwerewolf.room.service.RoomService;
import com.example.aiwerewolf.security.GodViewAccessService;
import com.example.aiwerewolf.speech.service.SpeechService;
import com.example.aiwerewolf.vote.service.VoteService;
import com.example.aiwerewolf.websocket.WebSocketPushService;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
class RoomControllerApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;
    @MockitoBean
    private GamePhaseEngine gamePhaseEngine;
    @MockitoBean
    private GameViewBuilder gameViewBuilder;
    @MockitoBean
    private SpeechService speechService;
    @MockitoBean
    private VoteService voteService;
    @MockitoBean
    private com.example.aiwerewolf.action.service.NightActionService nightActionService;
    @MockitoBean
    private DefaultConfigService defaultConfigService;
    @MockitoBean
    private RoleCatalogService roleCatalogService;
    @MockitoBean
    private WebSocketPushService pushService;
    @MockitoBean
    private GodViewAccessService godViewAccessService;
    @MockitoBean
    private AgentRunService agentRunService;
    @MockitoBean
    private AgentTaskService agentTaskService;
    @MockitoBean
    private GameEventService gameEventService;

    @Test
    void startRoomReturnsUnifiedResponse() throws Exception {
        when(roomService.startGame("room-1")).thenReturn(roomResponse(null));

        mockMvc.perform(post("/api/rooms/room-1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("room-1"))
                .andExpect(jsonPath("$.data.godViewToken").doesNotExist());
    }

    @Test
    void godViewRequiresToken() throws Exception {
        doThrow(new com.example.aiwerewolf.common.exception.BusinessException("ACCESS_DENIED", "访问上帝视角需要主持人令牌"))
                .when(godViewAccessService).verify("room-1", null);

        mockMvc.perform(get("/api/rooms/room-1/god-view"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void godViewWithTokenReturnsView() throws Exception {
        GameView view = new GameView("room-1", "room", RoomStatus.RUNNING, GamePhase.DAY_SPEECH, 1,
                null, null, null, List.of(), List.of(), List.of(), List.of(), true);
        when(gameViewBuilder.buildGodView("room-1")).thenReturn(view);

        mockMvc.perform(get("/api/rooms/room-1/god-view").header("X-God-View-Token", "token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.godView").value(true));
    }

    @Test
    void agentRunsRequireGodViewToken() throws Exception {
        doThrow(new com.example.aiwerewolf.common.exception.BusinessException("ACCESS_DENIED", "访问上帝视角需要主持人令牌"))
                .when(godViewAccessService).verify("room-1", null);

        mockMvc.perform(get("/api/rooms/room-1/agent-runs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void agentRunsWithTokenReturnsRecentRuns() throws Exception {
        when(agentRunService.listRecentForRoom("room-1")).thenReturn(List.of(new AgentRunResponse(
                "run-1", "room-1", "player-1", "player-1", 1, GamePhase.DAY_VOTE,
                AgentRunPurpose.VOTE, AgentRunStatus.FALLBACK, true, 2, 13,
                "role-prompts-v1:VILLAGER", "task-prompts-v1:VOTE/output-schema-v1:VOTE", "mock", "mock-json-v1",
                "{}", "{\"targetPlayerId\":\"player-2\"}", "fallback", Instant.now())));

        mockMvc.perform(get("/api/rooms/room-1/agent-runs").header("X-God-View-Token", "token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("run-1"))
                .andExpect(jsonPath("$.data[0].purpose").value("VOTE"))
                .andExpect(jsonPath("$.data[0].fallbackUsed").value(true));
    }

    @Test
    void agentTasksWithTokenReturnsRecentTasks() throws Exception {
        when(agentTaskService.listRecentForRoom("room-1")).thenReturn(List.of(new AgentTaskSnapshot(
                "task-1", "room-1", "player-1", 2, GamePhase.DAY_VOTE,
                AgentRunPurpose.VOTE, AgentTaskStatus.SUCCEEDED,
                Instant.now(), Instant.now(), Instant.now(), 12, null)));

        mockMvc.perform(get("/api/rooms/room-1/agent-tasks").header("X-God-View-Token", "token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].taskId").value("task-1"))
                .andExpect(jsonPath("$.data[0].purpose").value("VOTE"))
                .andExpect(jsonPath("$.data[0].status").value("SUCCEEDED"));
    }

    @Test
    void replayEndpointsReturnEventsWithGodReplayProtected() throws Exception {
        Instant now = Instant.now();
        when(gameEventService.listPublicReplay("room-1")).thenReturn(List.of(new GameEventResponse(
                "event-1", "room-1", 1, GamePhase.DAY_SPEECH, "SPEECH",
                "{\"content\":\"hello\"}", MemoryScope.PUBLIC, now)));
        when(gameEventService.listGodReplay("room-1")).thenReturn(List.of(new GameEventResponse(
                "event-2", "room-1", 1, GamePhase.SEER_ACTION, "SEER_CHECK",
                "{\"content\":\"secret\"}", MemoryScope.PRIVATE, now)));
        doThrow(new com.example.aiwerewolf.common.exception.BusinessException("ACCESS_DENIED", "访问上帝视角需要主持人令牌"))
                .when(godViewAccessService).verify("room-1", null);

        mockMvc.perform(get("/api/rooms/room-1/replay/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("event-1"))
                .andExpect(jsonPath("$.data[0].scope").value("PUBLIC"));

        mockMvc.perform(get("/api/rooms/room-1/replay/god"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/rooms/room-1/replay/god").header("X-God-View-Token", "token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("event-2"))
                .andExpect(jsonPath("$.data[0].scope").value("PRIVATE"));
    }

    @Test
    void humanSpeechUsesCurrentRoomRound() throws Exception {
        when(roomService.currentRound("room-1")).thenReturn(3);
        when(gameViewBuilder.buildPublicView("room-1")).thenReturn(new GameView("room-1", "room", RoomStatus.RUNNING, GamePhase.DAY_SPEECH, 3,
                null, null, null, List.of(), List.of(), List.of(), List.of(), false));

        mockMvc.perform(post("/api/rooms/room-1/players/player-1/speech")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"第二轮发言\",\"claimedRole\":null}"))
                .andExpect(status().isOk());

        verify(speechService).submitHumanSpeech(org.mockito.ArgumentMatchers.eq("room-1"), org.mockito.ArgumentMatchers.eq(3),
                org.mockito.ArgumentMatchers.eq("player-1"), any());
    }

    @Test
    void fullAiGameCanBeDrivenThroughApiSurface() throws Exception {
        RoomEntity gameOverRoom = roomEntity(GamePhase.GAME_OVER, RoomStatus.GAME_OVER);
        when(roomService.createRoom(any())).thenReturn(roomResponse("token"));
        when(roomService.startGame("room-1")).thenReturn(roomResponse(null));
        when(gamePhaseEngine.advanceUntilGameOver("room-1")).thenReturn(gameOverRoom);
        when(roomService.toResponse(gameOverRoom)).thenReturn(roomResponse(GamePhase.GAME_OVER, RoomStatus.GAME_OVER, null));
        when(gameViewBuilder.buildPublicView("room-1")).thenReturn(new GameView("room-1", "room", RoomStatus.GAME_OVER, GamePhase.GAME_OVER, 3,
                null, null, null,
                List.of(new PlayerView("p1", 1, "AI-1", PlayerType.AI, false, false, false, null, null)),
                List.of(), List.of(), List.of(), false));
        when(gameViewBuilder.buildGodView("room-1")).thenReturn(new GameView("room-1", "room", RoomStatus.GAME_OVER, GamePhase.GAME_OVER, 3,
                null, null, null,
                List.of(new PlayerView("p1", 1, "AI-1", PlayerType.AI, false, false, false, Role.WEREWOLF, Camp.WEREWOLF)),
                List.of(), List.of(), List.of(), true));
        doThrow(new com.example.aiwerewolf.common.exception.BusinessException("ACCESS_DENIED", "访问上帝视角需要主持人令牌"))
                .when(godViewAccessService).verify("room-1", null);

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sevenPlayerRoomJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.godViewToken").value("token"));

        mockMvc.perform(post("/api/rooms/room-1/start"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/rooms/room-1/simulate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("GAME_OVER"))
                .andExpect(jsonPath("$.data.phase").value("GAME_OVER"));

        mockMvc.perform(get("/api/rooms/room-1/public-view"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.players[0].role").doesNotExist())
                .andExpect(jsonPath("$.data.godView").value(false));

        mockMvc.perform(get("/api/rooms/room-1/god-view"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/rooms/room-1/god-view").header("X-God-View-Token", "token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.players[0].role").value("WEREWOLF"))
                .andExpect(jsonPath("$.data.godView").value(true));
    }

    private RoomResponse roomResponse(String token) {
        return roomResponse(GamePhase.FIRST_NIGHT, RoomStatus.RUNNING, token);
    }

    private RoomResponse roomResponse(GamePhase phase, RoomStatus status, String token) {
        Instant now = Instant.now();
        return new RoomResponse("room-1", "room", status, phase, 7,
                HumanMode.NONE, ObserverViewMode.GOD_VIEW, now, now, token);
    }

    private RoomEntity roomEntity(GamePhase phase, RoomStatus status) {
        RoomEntity room = new RoomEntity();
        room.setId("room-1");
        room.setName("room");
        room.setPhase(phase);
        room.setStatus(status);
        room.setTotalSeats(7);
        room.setHumanMode(HumanMode.NONE);
        room.setObserverViewMode(ObserverViewMode.GOD_VIEW);
        return room;
    }

    private String sevenPlayerRoomJson() {
        return """
                {
                  "roomName":"AI 7人标准局",
                  "totalSeats":7,
                  "humanMode":"NONE",
                  "humanPlayerName":"",
                  "humanRoleAssignMode":"RANDOM",
                  "specifiedHumanRole":null,
                  "observerViewMode":"GOD_VIEW",
                  "roleConfig":{"werewolfCount":2,"wolfKingCount":0,"whiteWolfKingCount":0,"hiddenWolfCount":0,"villagerCount":3,"seerCount":1,"witchCount":1,"hunterCount":0,"guardCount":0,"idiotCount":0,"knightCount":0,"graveKeeperCount":0,"magicianCount":0,"cupidCount":0,"elderCount":0},
                  "ruleConfig":{"victoryRule":"SLAUGHTER_SIDE","enableSheriff":false,"enableLastWords":true,"allowWitchSaveSelfFirstNight":true,"allowHunterShootWhenPoisoned":false,"allowGuardProtectSameTargetConsecutively":false,"allowWerewolfNightChat":true,"allowWhiteWolfKingExplode":true,"enableLovers":false,"speechTimeLimitSeconds":90,"voteTimeLimitSeconds":45,"nightActionTimeLimitSeconds":45,"aiThinkingDelayMillis":0,"autoAdvance":false,"revealRoleOnDeath":false},
                  "uiConfig":{"theme":"dark-moon","animationLevel":"smooth","enableSoundEffect":false,"showRoleAvatar":true,"showTimeline":true,"showGodViewPanel":true}
                }
                """;
    }
}
