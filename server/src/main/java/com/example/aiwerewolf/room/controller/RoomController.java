package com.example.aiwerewolf.room.controller;

import com.example.aiwerewolf.action.service.GameActionRequest;
import com.example.aiwerewolf.aiinfra.run.AgentRunResponse;
import com.example.aiwerewolf.aiinfra.run.AgentRunService;
import com.example.aiwerewolf.aiinfra.worker.AgentTaskService;
import com.example.aiwerewolf.aiinfra.worker.AgentTaskSnapshot;
import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.common.response.ApiResponse;
import com.example.aiwerewolf.game.event.GameEventResponse;
import com.example.aiwerewolf.game.event.GameEventService;
import com.example.aiwerewolf.game.engine.GamePhaseEngine;
import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.game.view.GameViewBuilder;
import com.example.aiwerewolf.role.config.DefaultConfigService;
import com.example.aiwerewolf.role.config.RoleCatalogService;
import com.example.aiwerewolf.role.config.RoleInfoResponse;
import com.example.aiwerewolf.room.dto.CreateRoomRequest;
import com.example.aiwerewolf.room.dto.DefaultConfigResponse;
import com.example.aiwerewolf.room.dto.RoomResponse;
import com.example.aiwerewolf.room.service.RoomService;
import com.example.aiwerewolf.security.GodViewAccessService;
import com.example.aiwerewolf.speech.service.SpeechRequest;
import com.example.aiwerewolf.speech.service.SpeechService;
import com.example.aiwerewolf.vote.service.VoteRequest;
import com.example.aiwerewolf.vote.service.VoteService;
import com.example.aiwerewolf.websocket.WebSocketPushService;
import jakarta.validation.Valid;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RoomController {
    private final RoomService roomService;
    private final GamePhaseEngine gamePhaseEngine;
    private final GameViewBuilder gameViewBuilder;
    private final SpeechService speechService;
    private final VoteService voteService;
    private final com.example.aiwerewolf.action.service.NightActionService nightActionService;
    private final DefaultConfigService defaultConfigService;
    private final RoleCatalogService roleCatalogService;
    private final WebSocketPushService pushService;
    private final GodViewAccessService godViewAccessService;
    private final AgentRunService agentRunService;
    private final AgentTaskService agentTaskService;
    private final GameEventService gameEventService;

    public RoomController(RoomService roomService,
                          GamePhaseEngine gamePhaseEngine,
                          GameViewBuilder gameViewBuilder,
                          SpeechService speechService,
                          VoteService voteService,
                          com.example.aiwerewolf.action.service.NightActionService nightActionService,
                          DefaultConfigService defaultConfigService,
                          RoleCatalogService roleCatalogService,
                          WebSocketPushService pushService,
                          GodViewAccessService godViewAccessService,
                          AgentRunService agentRunService,
                          AgentTaskService agentTaskService,
                          GameEventService gameEventService) {
        this.roomService = roomService;
        this.gamePhaseEngine = gamePhaseEngine;
        this.gameViewBuilder = gameViewBuilder;
        this.speechService = speechService;
        this.voteService = voteService;
        this.nightActionService = nightActionService;
        this.defaultConfigService = defaultConfigService;
        this.roleCatalogService = roleCatalogService;
        this.pushService = pushService;
        this.godViewAccessService = godViewAccessService;
        this.agentRunService = agentRunService;
        this.agentTaskService = agentTaskService;
        this.gameEventService = gameEventService;
    }

    @PostMapping("/rooms")
    public ApiResponse<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomResponse response = requireData(roomService.createRoom(request), "room response");
        String responseRoomId = requiredId(response.id(), "response.roomId");
        pushService.pushPublicEvent(responseRoomId, response);
        return ok(response);
    }

    @GetMapping("/rooms/{roomId}")
    public ApiResponse<RoomResponse> getRoom(@PathVariable String roomId) {
        return ok(roomService.getRoom(requiredId(roomId, "roomId")));
    }

    @PostMapping("/rooms/{roomId}/start")
    public ApiResponse<RoomResponse> start(@PathVariable String roomId) {
        String safeRoomId = requiredId(roomId, "roomId");
        RoomResponse response = requireData(roomService.startGame(safeRoomId), "room response");
        pushService.pushPhaseChanged(safeRoomId, response);
        return ok(response);
    }

    @PostMapping("/rooms/{roomId}/pause")
    public ApiResponse<RoomResponse> pause(@PathVariable String roomId) {
        return ok(roomService.pauseGame(requiredId(roomId, "roomId")));
    }

    @PostMapping("/rooms/{roomId}/resume")
    public ApiResponse<RoomResponse> resume(@PathVariable String roomId) {
        return ok(roomService.resumeGame(requiredId(roomId, "roomId")));
    }

    @PostMapping("/rooms/{roomId}/advance")
    public ApiResponse<RoomResponse> advance(@PathVariable String roomId) {
        String safeRoomId = requiredId(roomId, "roomId");
        RoomResponse response = requireData(roomService.toResponse(gamePhaseEngine.advancePhase(safeRoomId)), "room response");
        pushService.pushPhaseChanged(safeRoomId, response);
        return ok(response);
    }

    @PostMapping("/rooms/{roomId}/auto-advance")
    public ApiResponse<RoomResponse> autoAdvance(@PathVariable String roomId) {
        String safeRoomId = requiredId(roomId, "roomId");
        RoomResponse response = requireData(roomService.toResponse(gamePhaseEngine.advanceUntilHumanInputRequired(safeRoomId)), "room response");
        pushService.pushPhaseChanged(safeRoomId, response);
        return ok(response);
    }

    @PostMapping("/rooms/{roomId}/simulate")
    public ApiResponse<RoomResponse> simulate(@PathVariable String roomId) {
        String safeRoomId = requiredId(roomId, "roomId");
        RoomResponse response = requireData(roomService.toResponse(gamePhaseEngine.advanceUntilGameOver(safeRoomId)), "room response");
        pushService.pushPhaseChanged(safeRoomId, response);
        return ok(response);
    }

    @GetMapping("/rooms/{roomId}/public-view")
    public ApiResponse<GameView> publicView(@PathVariable String roomId) {
        return ok(gameViewBuilder.buildPublicView(requiredId(roomId, "roomId")));
    }

    @GetMapping("/rooms/{roomId}/players/{playerId}/private-view")
    public ApiResponse<GameView> privateView(@PathVariable String roomId, @PathVariable String playerId) {
        return ok(gameViewBuilder.buildPrivateView(requiredId(roomId, "roomId"), requiredId(playerId, "playerId")));
    }

    @GetMapping("/rooms/{roomId}/god-view")
    public ApiResponse<GameView> godView(@PathVariable String roomId, @RequestHeader(name = "X-God-View-Token", required = false) String token) {
        String safeRoomId = requiredId(roomId, "roomId");
        godViewAccessService.verify(safeRoomId, token);
        return ok(gameViewBuilder.buildGodView(safeRoomId));
    }

    @GetMapping("/rooms/{roomId}/agent-runs")
    public ApiResponse<List<AgentRunResponse>> agentRuns(@PathVariable String roomId,
                                                         @RequestHeader(name = "X-God-View-Token", required = false) String token) {
        String safeRoomId = requiredId(roomId, "roomId");
        godViewAccessService.verify(safeRoomId, token);
        return ok(agentRunService.listRecentForRoom(safeRoomId));
    }

    @GetMapping("/rooms/{roomId}/agent-tasks")
    public ApiResponse<List<AgentTaskSnapshot>> agentTasks(@PathVariable String roomId,
                                                           @RequestHeader(name = "X-God-View-Token", required = false) String token) {
        String safeRoomId = requiredId(roomId, "roomId");
        godViewAccessService.verify(safeRoomId, token);
        return ok(agentTaskService.listRecentForRoom(safeRoomId));
    }

    @PostMapping("/rooms/{roomId}/players/{playerId}/speech")
    public ApiResponse<Void> speech(@PathVariable String roomId, @PathVariable String playerId, @Valid @RequestBody SpeechRequest request) {
        String safeRoomId = requiredId(roomId, "roomId");
        speechService.submitHumanSpeech(safeRoomId, roomService.currentRound(safeRoomId), requiredId(playerId, "playerId"), request);
        GameView view = requireData(gameViewBuilder.buildPublicView(safeRoomId), "public view");
        pushService.pushTimelineUpdated(safeRoomId, view);
        return ApiResponse.ok(null);
    }

    @PostMapping("/rooms/{roomId}/players/{playerId}/vote")
    public ApiResponse<Void> vote(@PathVariable String roomId, @PathVariable String playerId, @Valid @RequestBody VoteRequest request) {
        String safeRoomId = requiredId(roomId, "roomId");
        voteService.submitHumanVote(safeRoomId, roomService.currentRound(safeRoomId), requiredId(playerId, "playerId"), request);
        GameView view = requireData(gameViewBuilder.buildPublicView(safeRoomId), "public view");
        pushService.pushTimelineUpdated(safeRoomId, view);
        return ApiResponse.ok(null);
    }

    @PostMapping("/rooms/{roomId}/players/{playerId}/night-action")
    public ApiResponse<Void> nightAction(@PathVariable String roomId, @PathVariable String playerId, @Valid @RequestBody GameActionRequest request) {
        String safeRoomId = requiredId(roomId, "roomId");
        nightActionService.submitHumanNightAction(safeRoomId, roomService.currentRound(safeRoomId), requiredId(playerId, "playerId"), request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/rooms/{roomId}/players/{playerId}/day-skill")
    public ApiResponse<Void> daySkill(@PathVariable String roomId, @PathVariable String playerId, @Valid @RequestBody GameActionRequest request) {
        return ApiResponse.ok(null);
    }

    @GetMapping("/rooms/{roomId}/timeline/public")
    public ApiResponse<GameView> publicTimeline(@PathVariable String roomId) {
        return ok(gameViewBuilder.buildPublicView(requiredId(roomId, "roomId")));
    }

    @GetMapping("/rooms/{roomId}/replay/public")
    public ApiResponse<List<GameEventResponse>> publicReplay(@PathVariable String roomId) {
        return ok(gameEventService.listPublicReplay(requiredId(roomId, "roomId")));
    }

    @GetMapping("/rooms/{roomId}/timeline/private/{playerId}")
    public ApiResponse<GameView> privateTimeline(@PathVariable String roomId, @PathVariable String playerId) {
        return ok(gameViewBuilder.buildPrivateView(requiredId(roomId, "roomId"), requiredId(playerId, "playerId")));
    }

    @GetMapping("/rooms/{roomId}/timeline/god")
    public ApiResponse<GameView> godTimeline(@PathVariable String roomId, @RequestHeader(name = "X-God-View-Token", required = false) String token) {
        return godView(roomId, token);
    }

    @GetMapping("/rooms/{roomId}/replay/god")
    public ApiResponse<List<GameEventResponse>> godReplay(@PathVariable String roomId,
                                                          @RequestHeader(name = "X-God-View-Token", required = false) String token) {
        String safeRoomId = requiredId(roomId, "roomId");
        godViewAccessService.verify(safeRoomId, token);
        return ok(gameEventService.listGodReplay(safeRoomId));
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleInfoResponse>> roles() {
        return ok(roleCatalogService.listRoles());
    }

    @GetMapping("/default-configs")
    public ApiResponse<DefaultConfigResponse> defaultConfigs() {
        return ok(defaultConfigService.defaults());
    }

    @NonNull
    private String requiredId(String value, String name) {
        if (value == null) {
            throw new BusinessException("VALIDATION_ERROR", name + " must not be null");
        }
        return value;
    }

    @NonNull
    private <T> ApiResponse<T> ok(T data) {
        T safeData = requireData(data, "response data");
        ApiResponse<T> response = ApiResponse.ok(safeData);
        if (response == null) {
            throw new BusinessException("INTERNAL_ERROR", "api response must not be null");
        }
        return response;
    }

    @NonNull
    private <T> T requireData(T data, String name) {
        if (data == null) {
            throw new BusinessException("INTERNAL_ERROR", name + " must not be null");
        }
        return data;
    }
}
