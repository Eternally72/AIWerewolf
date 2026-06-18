package com.example.aiwerewolf.aiinfra.worker;

import java.util.concurrent.CompletableFuture;

public record AgentTaskHandle<T>(
        AgentTaskEntity task,
        CompletableFuture<AgentTaskResult<T>> future
) {
}
