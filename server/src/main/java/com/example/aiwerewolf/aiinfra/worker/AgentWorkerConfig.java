package com.example.aiwerewolf.aiinfra.worker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AgentWorkerConfig {
    @Bean(name = "agentTaskExecutor")
    public Executor agentTaskExecutor(@Value("${ai-infra.agent-worker.pool-size:4}") int poolSize,
                                      @Value("${ai-infra.agent-worker.max-pool-size:8}") int maxPoolSize,
                                      @Value("${ai-infra.agent-worker.queue-capacity:128}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(1, poolSize));
        executor.setMaxPoolSize(Math.max(Math.max(1, poolSize), maxPoolSize));
        executor.setQueueCapacity(Math.max(1, queueCapacity));
        executor.setThreadNamePrefix("agent-worker-");
        executor.initialize();
        return executor;
    }
}
