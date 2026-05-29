package com.example.aiwerewolf.room.controller;

import com.example.aiwerewolf.action.service.GameActionRequest;
import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.common.response.ApiResponse;
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
import com.example.aiwerewolf.speech.service.SpeechRequest;
import com.example.aiwerewolf.speech.service.SpeechService;
import com.example.aiwerewolf.vote.service.VoteRequest;
import com.example.aiwerewolf.vote.service.VoteService;
import com.example.aiwerewolf.websocket.WebSocketPushService;
import jakarta.validation.Valid;
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

    public RoomController(RoomService roomService,
                          GamePhaseEngine gamePhaseEngine,
                          GameViewBuilder gameViewBuilder,
                          SpeechService speechService,
                          VoteService voteService,
                          com.example.aiwerewolf.action.service.NightActionService nightActionService,
                          DefaultConfigService defaultConfigService,
                          RoleCatalogService roleCatalogService,
                          WebSocketPushService pushService) {
        this.roomService = roomService;
        this.gamePhaseEngine = gamePhaseEngine;
        this.gameViewBuilder = gameViewBuilder;
        this.speechService = speechService;
        this.voteService = voteService;
        this.nightActionService = nightActionService;
        this.defaultConfigService = defaultConfigService;
        this.roleCatalogService = roleCatalogService;
        this.pushService = pushService;
    }

    @PostMapping("/rooms")
    public ApiResponse<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomResponse response = roomService.createRoom(request);
        pushService.pushPublicEvent(response.id(), response);
        return ApiResponse.ok(response);
    }

    @GetMapping("/rooms/{roomId}")
    public ApiResponse<RoomResponse> getRoom(@PathVariable String roomId) {
        return ApiResponse.ok(roomService.getRoom(roomId));
    }

    @PostMapping("/rooms/{roomId}/start")
    public ApiResponse<RoomResponse> start(@PathVariable String roomId) {
        RoomResponse response = roomService.startGame(roomId);
        pushService.pushPhaseChanged(roomId, response);
        return ApiResponse.ok(response);
    }

    @PostMapping("/rooms/{roomId}/pause")
    public ApiResponse<RoomResponse> pause(@PathVariable String roomId) {
        return ApiResponse.ok(roomService.pauseGame(roomId));
    }

    @PostMapping("/rooms/{roomId}/resume")
    public ApiResponse<RoomResponse> resume(@PathVariable String roomId) {
        return ApiResponse.ok(roomService.resumeGame(roomId));
    }

    @PostMapping("/rooms/{roomId}/advance")
    public ApiResponse<RoomResponse> advance(@PathVariable String roomId) {
        RoomResponse response = roomService.toResponse(gamePhaseEngine.advancePhase(roomId));
        pushService.pushPhaseChanged(roomId, response);
        return ApiResponse.ok(response);
    }

    @PostMapping("/rooms/{roomId}/auto-advance")
    public ApiResponse<RoomResponse> autoAdvance(@PathVariable String roomId) {
        RoomResponse response = roomService.toResponse(gamePhaseEngine.advanceUntilHumanInputRequired(roomId));
        pushService.pushPhaseChanged(roomId, response);
        return ApiResponse.ok(response);
    }

    @GetMapping("/rooms/{roomId}/public-view")
    public ApiResponse<GameView> publicView(@PathVariable String roomId) {
        return ApiResponse.ok(gameViewBuilder.buildPublicView(roomId));
    }

    @GetMapping("/rooms/{roomId}/players/{playerId}/private-view")
    public ApiResponse<GameView> privateView(@PathVariable String roomId, @PathVariable String playerId) {
        return ApiResponse.ok(gameViewBuilder.buildPrivateView(roomId, playerId));
    }

    @GetMapping("/rooms/{roomId}/god-view")
    public ApiResponse<GameView> godView(@PathVariable String roomId, @RequestParam(defaultValue = "false") boolean god) {
        if (!god) {
            throw new BusinessException("ACCESS_DENIED", "普通玩家不能访问上帝视角");
        }
        return ApiResponse.ok(gameViewBuilder.buildGodView(roomId));
    }

    @PostMapping("/rooms/{roomId}/players/{playerId}/speech")
    public ApiResponse<Void> speech(@PathVariable String roomId, @PathVariable String playerId, @Valid @RequestBody SpeechRequest request) {
        speechService.submitHumanSpeech(roomId, 1, playerId, request);
        pushService.pushTimelineUpdated(roomId, gameViewBuilder.buildPublicView(roomId));
        return ApiResponse.ok(null);
    }

    @PostMapping("/rooms/{roomId}/players/{playerId}/vote")
    public ApiResponse<Void> vote(@PathVariable String roomId, @PathVariable String playerId, @Valid @RequestBody VoteRequest request) {
        voteService.submitHumanVote(roomId, 1, playerId, request);
        pushService.pushTimelineUpdated(roomId, gameViewBuilder.buildPublicView(roomId));
        return ApiResponse.ok(null);
    }

    @PostMapping("/rooms/{roomId}/players/{playerId}/night-action")
    public ApiResponse<Void> nightAction(@PathVariable String roomId, @PathVariable String playerId, @Valid @RequestBody GameActionRequest request) {
        nightActionService.submitHumanNightAction(roomId, 1, playerId, request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/rooms/{roomId}/players/{playerId}/day-skill")
    public ApiResponse<Void> daySkill(@PathVariable String roomId, @PathVariable String playerId, @Valid @RequestBody GameActionRequest request) {
        return ApiResponse.ok(null);
    }

    @GetMapping("/rooms/{roomId}/timeline/public")
    public ApiResponse<GameView> publicTimeline(@PathVariable String roomId) {
        return ApiResponse.ok(gameViewBuilder.buildPublicView(roomId));
    }

    @GetMapping("/rooms/{roomId}/timeline/private/{playerId}")
    public ApiResponse<GameView> privateTimeline(@PathVariable String roomId, @PathVariable String playerId) {
        return ApiResponse.ok(gameViewBuilder.buildPrivateView(roomId, playerId));
    }

    @GetMapping("/rooms/{roomId}/timeline/god")
    public ApiResponse<GameView> godTimeline(@PathVariable String roomId, @RequestParam(defaultValue = "false") boolean god) {
        return godView(roomId, god);
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleInfoResponse>> roles() {
        return ApiResponse.ok(roleCatalogService.listRoles());
    }

    @GetMapping("/default-configs")
    public ApiResponse<DefaultConfigResponse> defaultConfigs() {
        return ApiResponse.ok(defaultConfigService.defaults());
    }
}
