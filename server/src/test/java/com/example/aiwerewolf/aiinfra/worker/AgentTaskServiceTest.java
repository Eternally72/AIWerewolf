package com.example.aiwerewolf.aiinfra.worker;

import com.example.aiwerewolf.aiinfra.observability.AiInfraMetrics;
import com.example.aiwerewolf.aiinfra.run.AgentRunPurpose;
import com.example.aiwerewolf.game.phase.GamePhase;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentTaskServiceTest {
    @Test
    void executeRunsSynchronouslyAndTracksTaskLifecycleAndMetrics() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentTaskRepository repository = repository();
        AgentTaskService service = new AgentTaskService(
                repository,
                new AiInfraMetrics(meterRegistry));
        Thread callerThread = Thread.currentThread();
        AtomicReference<Thread> executionThread = new AtomicReference<>();

        String result = service.execute(
                new AgentTaskRequest("room-1", "player-1", 1, GamePhase.DAY_VOTE, AgentRunPurpose.VOTE),
                () -> {
                    executionThread.set(Thread.currentThread());
                    return "ok";
                });

        assertThat(result).isEqualTo("ok");
        assertThat(executionThread.get()).isSameAs(callerThread);
        assertThat(service.listRecentForRoom("room-1"))
                .singleElement()
                .satisfies(task -> {
                    assertThat(task.status()).isEqualTo(AgentTaskStatus.SUCCEEDED);
                    assertThat(task.purpose()).isEqualTo(AgentRunPurpose.VOTE);
                });
        assertThat(meterRegistry.counter("aiwerewolf.agent.tasks", "purpose", "vote", "status", "queued").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("aiwerewolf.agent.tasks", "purpose", "vote", "status", "succeeded").count()).isEqualTo(1.0);
    }

    private AgentTaskRepository repository() {
        AgentTaskRepository repository = mock(AgentTaskRepository.class);
        AtomicReference<AgentTaskEntity> stored = new AtomicReference<>();
        when(repository.save(any(AgentTaskEntity.class))).thenAnswer(invocation -> {
            AgentTaskEntity entity = invocation.getArgument(0);
            entity.prePersist();
            stored.set(entity);
            return entity;
        });
        when(repository.findTop100ByRoomIdOrderByQueuedAtDesc("room-1")).thenAnswer(invocation ->
                stored.get() == null ? List.of() : List.of(stored.get()));
        return repository;
    }
}
