package com.example.aiwerewolf.game.runtime;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService extends RedisOperationSupport {
    private final Set<String> localKeys = ConcurrentHashMap.newKeySet();

    public IdempotencyService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        super(redisTemplateProvider);
    }

    public boolean markIfAbsent(String key, Duration ttl) {
        return redis(template -> Boolean.TRUE.equals(template.opsForValue().setIfAbsent(key, "1", ttl)))
                .orElseGet(() -> localKeys.add(key));
    }
}
