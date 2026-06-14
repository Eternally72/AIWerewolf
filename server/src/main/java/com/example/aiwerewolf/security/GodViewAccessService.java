package com.example.aiwerewolf.security;

import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.game.runtime.RedisOperationSupport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GodViewAccessService extends RedisOperationSupport {
    private static final Duration TOKEN_TTL = Duration.ofDays(7);
    private final Map<String, String> localTokens = new ConcurrentHashMap<>();

    public GodViewAccessService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        super(redisTemplateProvider);
    }

    public String issueToken(String roomId) {
        String token = UUID.randomUUID().toString();
        localTokens.put(roomId, token);
        redis(template -> {
            template.opsForValue().set(key(roomId), token, TOKEN_TTL);
            return true;
        });
        return token;
    }

    public void verify(String roomId, @Nullable String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("ACCESS_DENIED", "访问上帝视角需要主持人令牌");
        }
        String expected = redis(template -> template.opsForValue().get(key(roomId)))
                .orElse(localTokens.get(roomId));
        if (!token.equals(expected)) {
            throw new BusinessException("ACCESS_DENIED", "上帝视角令牌无效");
        }
    }

    private String key(String roomId) {
        return "room:%s:god-token".formatted(roomId);
    }
}
