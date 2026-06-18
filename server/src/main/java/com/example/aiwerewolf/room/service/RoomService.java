package com.example.aiwerewolf.room.service;

import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.runtime.GameRuntimeStateCache;
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
import com.example.aiwerewolf.security.GodViewAccessService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final GodViewAccessService godViewAccessService;
    private final GameRuntimeStateCache runtimeStateCache;

    public RoomService(RoomRepository roomRepository,
                       GameConfigRepository gameConfigRepository,
                       RoleAssignmentService roleAssignmentService,
                       PlayerRepository playerRepository,
                       MemoryService memoryService,
                       ObjectMapper objectMapper,
                       GodViewAccessService godViewAccessService,
                       GameRuntimeStateCache runtimeStateCache) {
        this.roomRepository = roomRepository;
        this.gameConfigRepository = gameConfigRepository;
        this.roleAssignmentService = roleAssignmentService;
        this.playerRepository = playerRepository;
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
        this.godViewAccessService = godViewAccessService;
        this.runtimeStateCache = runtimeStateCache;
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
        runtimeStateCache.put(room);

        GameConfigEntity config = new GameConfigEntity();
        config.setRoomId(room.getId());
        config.setRoleConfigJson(writeJson(request.roleConfig()));
        config.setRuleConfigJson(writeJson(request.ruleConfig()));
        config.setUiConfigJson(writeJson(request.uiConfig()));
        gameConfigRepository.save(config);

        memoryService.appendPublicMemory(room.getId(), 1, GamePhase.WAITING, "ROOM_CREATED", "房间已创建：" + room.getName());
        return toResponse(room, godViewAccessService.issueToken(room.getId()));
    }

    public RoomResponse getRoom(String roomId) {
        return toResponse(room(roomId));
    }

    @Transactional
    public RoomResponse startGame(String roomId) {
        RoomEntity room = room(roomId);
        if (playerRepository.findByRoomIdOrderBySeatNumberAsc(roomId).isEmpty()
                && room.getStatus() != RoomStatus.GAME_OVER
                && room.getStatus() != RoomStatus.PAUSED) {
            roleAssignmentService.assignRoles(roomId, rebuildRequest(room));
        }
        if (room.getStatus() != RoomStatus.WAITING && room.getStatus() != RoomStatus.ROLE_ASSIGNED) {
            return toResponse(room);
        }
        CreateRoomRequest request = rebuildRequest(room);
        room.setStatus(RoomStatus.RUNNING);
        room.setPhase(GamePhase.FIRST_NIGHT);
        roomRepository.save(room);
        runtimeStateCache.put(room);
        memoryService.appendPublicMemory(roomId, room.getCurrentRound(), GamePhase.FIRST_NIGHT, "GAME_STARTED", "游戏开始，进入首夜");
        memoryService.appendGodViewMemory(roomId, room.getCurrentRound(), GamePhase.ROLE_ASSIGNED, "ROLE_ASSIGNED", "所有玩家身份已分配");
        return toResponse(room(roomId));
    }

    @Transactional
    public RoomResponse pauseGame(String roomId) {
        RoomEntity room = room(roomId);
        room.setStatus(RoomStatus.PAUSED);
        RoomEntity saved = roomRepository.save(room);
        runtimeStateCache.put(saved);
        return toResponse(saved);
    }

    @Transactional
    public RoomResponse resumeGame(String roomId) {
        RoomEntity room = room(roomId);
        room.setStatus(RoomStatus.RUNNING);
        RoomEntity saved = roomRepository.save(room);
        runtimeStateCache.put(saved);
        return toResponse(saved);
    }

    public RoomResponse getRoomStatus(String roomId) {
        return getRoom(roomId);
    }

    public int currentRound(String roomId) {
        return room(roomId).getCurrentRound();
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
        return toResponse(room, null);
    }

    public RoomResponse toResponse(RoomEntity room, String godViewToken) {
        return new RoomResponse(room.getId(), room.getName(), room.getStatus(), room.getPhase(), room.getTotalSeats(),
                room.getHumanMode(), room.getObserverViewMode(), room.getCreatedAt(), room.getUpdatedAt(), godViewToken);
    }
}
