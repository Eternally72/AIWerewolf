package com.example.aiwerewolf.game.runtime;

import com.example.aiwerewolf.common.exception.BusinessException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
public class PhaseAdvanceLockService extends RedisOperationSupport {
    private static final Duration LOCK_TTL = Duration.ofSeconds(20);
    private final Map<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    public PhaseAdvanceLockService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        super(redisTemplateProvider);
    }

    public <T> T withRoomLock(String roomId, Supplier<T> work) {
        ReentrantLock localLock = localLocks.computeIfAbsent(roomId, ignored -> new ReentrantLock());
        if (!localLock.tryLock()) {
            throw new BusinessException("ROOM_BUSY", "房间正在推进阶段，请稍后重试");
        }
        String redisKey = "room:%s:phase-lock".formatted(roomId);
        boolean redisLocked = redis(template -> Boolean.TRUE.equals(
                template.opsForValue().setIfAbsent(redisKey, "locked", LOCK_TTL))).orElse(true);
        if (!redisLocked) {
            localLock.unlock();
            throw new BusinessException("ROOM_BUSY", "房间正在推进阶段，请稍后重试");
        }
        try {
            return work.get();
        } finally {
            redis(template -> {
                template.delete(redisKey);
                return true;
            });
            localLock.unlock();
        }
    }
}
