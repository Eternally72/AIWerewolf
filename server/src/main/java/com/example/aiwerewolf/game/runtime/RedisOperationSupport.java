package com.example.aiwerewolf.game.runtime;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.function.Function;

public abstract class RedisOperationSupport {
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    protected RedisOperationSupport(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    protected <T> Optional<T> redis(Function<StringRedisTemplate, T> operation) {
        StringRedisTemplate template = redisTemplateProvider.getIfAvailable();
        if (template == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(operation.apply(template));
        } catch (RedisConnectionFailureException | IllegalStateException ex) {
            return Optional.empty();
        }
    }
}
