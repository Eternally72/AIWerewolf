package com.example.aiwerewolf.game.runtime;

import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.room.entity.RoomEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameRuntimeStateCache extends RedisOperationSupport {
    private static final Duration TTL = Duration.ofHours(12);
    private final ObjectMapper objectMapper;
    private final Map<String, RuntimeSnapshot> localSnapshots = new ConcurrentHashMap<>();

    public GameRuntimeStateCache(ObjectProvider<StringRedisTemplate> redisTemplateProvider, ObjectMapper objectMapper) {
        super(redisTemplateProvider);
        this.objectMapper = objectMapper;
    }

    public void put(RoomEntity room) {
        RuntimeSnapshot snapshot = new RuntimeSnapshot(room.getId(), room.getPhase(), room.getCurrentRound(), room.getStatus().name());
        localSnapshots.put(room.getId(), snapshot);
        redis(template -> {
            try {
                template.opsForValue().set(key(room.getId()), objectMapper.writeValueAsString(snapshot), TTL);
            } catch (JsonProcessingException ignored) {
                return false;
            }
            return true;
        });
    }

    public Optional<RuntimeSnapshot> get(String roomId) {
        Optional<RuntimeSnapshot> redisSnapshot = redis(template -> parse(template.opsForValue().get(key(roomId))))
                .flatMap(snapshot -> snapshot);
        if (redisSnapshot.isPresent()) {
            return redisSnapshot;
        }
        return Optional.ofNullable(localSnapshots.get(roomId));
    }

    private Optional<RuntimeSnapshot> parse(@Nullable String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, RuntimeSnapshot.class));
        } catch (JsonProcessingException ex) {
            return Optional.empty();
        }
    }

    private String key(String roomId) {
        return "room:%s:runtime".formatted(roomId);
    }

    public record RuntimeSnapshot(String roomId, GamePhase phase, int roundNumber, String status) {
    }
}
