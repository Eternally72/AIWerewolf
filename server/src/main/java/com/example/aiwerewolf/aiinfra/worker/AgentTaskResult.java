package com.example.aiwerewolf.aiinfra.worker;

public record AgentTaskResult<T>(
        AgentTaskSnapshot task,
        T result
) {
}
