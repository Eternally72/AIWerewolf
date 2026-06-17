package com.example.aiwerewolf.game.event;

import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GameEventService {
    private final GameEventRepository repository;
    private final ObjectMapper objectMapper;

    public GameEventService(GameEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public GameEventEntity append(String roomId,
                                  int round,
                                  GamePhase phase,
                                  String eventType,
                                  MemoryScope scope,
                                  Object payload) {
        GameEventEntity event = new GameEventEntity();
        event.setRoomId(roomId);
        event.setRoundNumber(round);
        event.setPhase(phase);
        event.setEventType(eventType);
        event.setScope(scope);
        event.setPayloadJson(toJson(payload));
        return repository.save(event);
    }

    public GameEventEntity appendMemoryEvent(String roomId,
                                             int round,
                                             GamePhase phase,
                                             MemoryScope scope,
                                             @Nullable String ownerPlayerId,
                                             @Nullable String visibleToPlayerIds,
                                             String eventType,
                                             String content,
                                             @Nullable String metadataJson) {
        return append(roomId, round, phase, eventType, scope, Map.of(
                "content", content,
                "ownerPlayerId", ownerPlayerId == null ? "" : ownerPlayerId,
                "visibleToPlayerIds", visibleToPlayerIds == null ? "" : visibleToPlayerIds,
                "metadataJson", metadataJson == null ? "{}" : metadataJson));
    }

    public List<GameEventResponse> listPublicReplay(String roomId) {
        return repository.findByRoomIdAndScopeOrderByCreatedAtAsc(roomId, MemoryScope.PUBLIC).stream()
                .map(GameEventResponse::fromEntity)
                .toList();
    }

    public List<GameEventResponse> listGodReplay(String roomId) {
        return repository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(GameEventResponse::fromEntity)
                .toList();
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"payload serialization failed\"}";
        }
    }
}
