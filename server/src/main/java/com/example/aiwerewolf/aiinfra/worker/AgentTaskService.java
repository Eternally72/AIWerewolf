package com.example.aiwerewolf.aiinfra.worker;

import com.example.aiwerewolf.aiinfra.observability.AiInfraMetrics;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

@Service
public class AgentTaskService {
    private final AgentTaskRepository repository;
    private final AiInfraMetrics metrics;

    public AgentTaskService(AgentTaskRepository repository,
                            AiInfraMetrics metrics) {
        this.repository = repository;
        this.metrics = metrics;
    }

    public <T> T execute(AgentTaskRequest request, Supplier<T> supplier) {
        AgentTaskEntity task = createTask(request);
        // 游戏推进必须严格保持座位顺序；模型调用直接在当前线程执行，禁止后续 Agent 抢先读取上下文。
        return runTask(task, supplier).result();
    }

    private AgentTaskEntity createTask(AgentTaskRequest request) {
        AgentTaskEntity task = repository.save(AgentTaskEntity.queued(request));
        metrics.recordAgentTask(request.purpose(), AgentTaskStatus.QUEUED, 0);
        return task;
    }

    public List<AgentTaskSnapshot> listRecentForRoom(String roomId) {
        return repository.findTop100ByRoomIdOrderByQueuedAtDesc(roomId).stream()
                .map(AgentTaskEntity::snapshot)
                .toList();
    }

    private <T> AgentTaskResult<T> runTask(AgentTaskEntity task, Supplier<T> supplier) {
        task.markRunning();
        updateTaskLifecycle(task);
        metrics.recordAgentTask(task.getPurpose(), AgentTaskStatus.RUNNING, 0);
        try {
            T result = supplier.get();
            if (task.getStatus() != AgentTaskStatus.TIMED_OUT) {
                task.markSucceeded();
                updateTaskLifecycle(task);
                metrics.recordAgentTask(task.getPurpose(), AgentTaskStatus.SUCCEEDED, task.getLatencyMillis());
            }
            return new AgentTaskResult<>(task.snapshot(), result);
        } catch (RuntimeException ex) {
            if (task.getStatus() != AgentTaskStatus.TIMED_OUT) {
                task.markFailed(safeError(ex));
                updateTaskLifecycle(task);
                metrics.recordAgentTask(task.getPurpose(), AgentTaskStatus.FAILED, task.getLatencyMillis());
            }
            throw ex;
        }
    }

    private void updateTaskLifecycle(AgentTaskEntity task) {
        repository.updateLifecycle(
                task.getId(),
                task.getStatus(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getLatencyMillis(),
                task.getErrorMessage(),
                Instant.now());
    }

    private String safeError(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

}
