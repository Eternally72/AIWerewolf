package com.example.aiwerewolf.room.service;

import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.game.engine.GamePhaseEngine;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.memory.service.MemoryService;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.role.service.RoleAssignmentService;
import com.example.aiwerewolf.room.dto.CreateRoomRequest;
import com.example.aiwerewolf.room.dto.RoomResponse;
import com.example.aiwerewolf.room.entity.GameConfigEntity;
import com.example.aiwerewolf.room.entity.RoomEntity;
import com.example.aiwerewolf.room.entity.RoomStatus;
import com.example.aiwerewolf.room.repository.GameConfigRepository;
import com.example.aiwerewolf.room.repository.RoomRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final GameConfigRepository gameConfigRepository;
    private final RoleAssignmentService roleAssignmentService;
    private final PlayerRepository playerRepository;
    private final MemoryService memoryService;
    private final ObjectMapper objectMapper;
    private final GamePhaseEngine gamePhaseEngine;

    public RoomService(RoomRepository roomRepository,
                       GameConfigRepository gameConfigRepository,
                       RoleAssignmentService roleAssignmentService,
                       PlayerRepository playerRepository,
                       MemoryService memoryService,
                       ObjectMapper objectMapper,
                       @Lazy GamePhaseEngine gamePhaseEngine) {
        this.roomRepository = roomRepository;
        this.gameConfigRepository = gameConfigRepository;
        this.roleAssignmentService = roleAssignmentService;
        this.playerRepository = playerRepository;
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
        this.gamePhaseEngine = gamePhaseEngine;
    }

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        roleAssignmentService.validateRoleConfig(request);
        RoomEntity room = new RoomEntity();
        room.setName(request.roomName());
        room.setTotalSeats(request.totalSeats());
        room.setHumanMode(request.humanMode());
        room.setObserverViewMode(request.observerViewMode());
        room.setStatus(RoomStatus.WAITING);
        room.setPhase(GamePhase.WAITING);
        room = roomRepository.save(room);

        GameConfigEntity config = new GameConfigEntity();
        config.setRoomId(room.getId());
        config.setRoleConfigJson(writeJson(request.roleConfig()));
        config.setRuleConfigJson(writeJson(request.ruleConfig()));
        config.setUiConfigJson(writeJson(request.uiConfig()));
        gameConfigRepository.save(config);

        memoryService.appendPublicMemory(room.getId(), 1, GamePhase.WAITING, "ROOM_CREATED", "房间已创建：" + room.getName());
        return toResponse(room);
    }

    public RoomResponse getRoom(String roomId) {
        return toResponse(room(roomId));
    }

    @Transactional
    public RoomResponse startGame(String roomId) {
        RoomEntity room = room(roomId);
        if (room.getStatus() != RoomStatus.WAITING && room.getStatus() != RoomStatus.ROLE_ASSIGNED) {
            throw new BusinessException("ILLEGAL_PHASE_OPERATION", "游戏已经开始，不能重复开始");
        }
        CreateRoomRequest request = rebuildRequest(room);
        if (playerRepository.findByRoomIdOrderBySeatNumberAsc(roomId).isEmpty()) {
            roleAssignmentService.assignRoles(roomId, request);
        }
        room.setStatus(RoomStatus.RUNNING);
        room.setPhase(GamePhase.FIRST_NIGHT);
        roomRepository.save(room);
        memoryService.appendPublicMemory(roomId, room.getCurrentRound(), GamePhase.FIRST_NIGHT, "GAME_STARTED", "游戏开始，进入首夜");
        memoryService.appendGodViewMemory(roomId, room.getCurrentRound(), GamePhase.ROLE_ASSIGNED, "ROLE_ASSIGNED", "所有玩家身份已分配");
        if (request.ruleConfig().autoAdvance()) {
            gamePhaseEngine.advanceUntilHumanInputRequired(roomId);
        }
        return toResponse(room(roomId));
    }

    @Transactional
    public RoomResponse pauseGame(String roomId) {
        RoomEntity room = room(roomId);
        room.setStatus(RoomStatus.PAUSED);
        return toResponse(roomRepository.save(room));
    }

    @Transactional
    public RoomResponse resumeGame(String roomId) {
        RoomEntity room = room(roomId);
        room.setStatus(RoomStatus.RUNNING);
        return toResponse(roomRepository.save(room));
    }

    public RoomResponse getRoomStatus(String roomId) {
        return getRoom(roomId);
    }

    private CreateRoomRequest rebuildRequest(RoomEntity room) {
        GameConfigEntity config = gameConfigRepository.findByRoomId(room.getId())
                .orElseThrow(() -> new BusinessException("ROOM_NOT_FOUND", "房间配置不存在"));
        try {
            return new CreateRoomRequest(
                    room.getName(),
                    room.getTotalSeats(),
                    room.getHumanMode(),
                    null,
                    com.example.aiwerewolf.room.dto.HumanRoleAssignMode.RANDOM,
                    null,
                    room.getObserverViewMode(),
                    objectMapper.readValue(config.getRoleConfigJson(), com.example.aiwerewolf.room.dto.RoleConfig.class),
                    objectMapper.readValue(config.getRuleConfigJson(), com.example.aiwerewolf.room.dto.RuleConfig.class),
                    objectMapper.readValue(config.getUiConfigJson(), com.example.aiwerewolf.room.dto.UiConfig.class)
            );
        } catch (JsonProcessingException ex) {
            throw new BusinessException("CONFIG_ERROR", "房间配置解析失败");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("CONFIG_ERROR", "配置序列化失败");
        }
    }

    private RoomEntity room(String roomId) {
        return roomRepository.findById(Objects.requireNonNull(roomId, "roomId must not be null"))
                .orElseThrow(() -> new BusinessException("ROOM_NOT_FOUND", "房间不存在"));
    }

    public RoomResponse toResponse(RoomEntity room) {
        return new RoomResponse(room.getId(), room.getName(), room.getStatus(), room.getPhase(), room.getTotalSeats(),
                room.getHumanMode(), room.getObserverViewMode(), room.getCreatedAt(), room.getUpdatedAt());
    }
}
