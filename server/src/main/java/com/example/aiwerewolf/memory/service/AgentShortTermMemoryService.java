package com.example.aiwerewolf.memory.service;

import com.example.aiwerewolf.game.runtime.RedisOperationSupport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentShortTermMemoryService extends RedisOperationSupport {
    private static final Duration TTL = Duration.ofHours(6);
    private static final int MAX_ITEMS = 40;
    private final Map<String, Deque<String>> localMemory = new ConcurrentHashMap<>();

    public AgentShortTermMemoryService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        super(redisTemplateProvider);
    }

    public void appendObservation(String roomId, String playerId, String content) {
        String safeContent = content == null ? "" : content.strip();
        if (safeContent.isBlank()) {
            return;
        }
        String key = key(roomId, playerId);
        boolean redisOk = redis(template -> {
            template.opsForList().rightPush(key, safeContent);
            template.opsForList().trim(key, -MAX_ITEMS, -1);
            template.expire(key, TTL);
            return true;
        }).orElse(false);
        if (!redisOk) {
            Deque<String> deque = localMemory.computeIfAbsent(key, ignored -> new ArrayDeque<>());
            synchronized (deque) {
                deque.addLast(safeContent);
                while (deque.size() > MAX_ITEMS) {
                    deque.removeFirst();
                }
            }
        }
    }

    public List<String> listRecent(String roomId, String playerId) {
        String key = key(roomId, playerId);
        List<String> redisValues = redis(template -> template.opsForList().range(key, 0, -1))
                .orElse(List.of());
        if (!redisValues.isEmpty()) {
            return redisValues;
        }
        Deque<String> deque = localMemory.get(key);
        if (deque == null) {
            return List.of();
        }
        synchronized (deque) {
            return new ArrayList<>(deque);
        }
    }

    private String key(String roomId, String playerId) {
        return "agent:%s:%s:stm".formatted(roomId, playerId);
    }
}
