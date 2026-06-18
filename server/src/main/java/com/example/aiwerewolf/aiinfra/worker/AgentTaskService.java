package com.example.aiwerewolf.aiinfra.worker;

import com.example.aiwerewolf.aiinfra.observability.AiInfraMetrics;
import com.example.aiwerewolf.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Service
public class AgentTaskService {
    private final Executor executor;
    private final AgentTaskRepository repository;
    private final AiInfraMetrics metrics;
    private final Duration awaitTimeout;

    public AgentTaskService(@Qualifier("agentTaskExecutor") Executor executor,
                            AgentTaskRepository repository,
                            AiInfraMetrics metrics,
                            @Value("${ai-infra.agent-worker.await-timeout-seconds:60}") long awaitTimeoutSeconds) {
        this.executor = executor;
        this.repository = repository;
        this.metrics = metrics;
        this.awaitTimeout = Duration.ofSeconds(Math.max(1, awaitTimeoutSeconds));
    }

    public <T> T submitAndAwait(AgentTaskRequest request, Supplier<T> supplier) {
        return await(submitHandle(request, supplier)).result();
    }

    public <T> CompletableFuture<AgentTaskResult<T>> submit(AgentTaskRequest request, Supplier<T> supplier) {
        return submitHandle(request, supplier).future();
    }

    public <T> AgentTaskHandle<T> submitHandle(AgentTaskRequest request, Supplier<T> supplier) {
        AgentTaskEntity task = createTask(request);
        return new AgentTaskHandle<>(task, execute(task, supplier));
    }

    public <T> AgentTaskResult<T> await(AgentTaskHandle<T> handle) {
        AgentTaskEntity task = handle.task();
        CompletableFuture<AgentTaskResult<T>> future = handle.future();
        try {
            return future.get(awaitTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            task.markTimedOut();
            updateTaskLifecycle(task);
            metrics.recordAgentTask(task.getPurpose(), AgentTaskStatus.TIMED_OUT, task.getLatencyMillis());
            throw new BusinessException("AGENT_TASK_TIMEOUT", "AI Agent 任务执行超时");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException("AGENT_TASK_FAILED", safeError(cause == null ? ex : cause));
        }
    }

    private AgentTaskEntity createTask(AgentTaskRequest request) {
        AgentTaskEntity task = repository.save(AgentTaskEntity.queued(request));
        metrics.recordAgentTask(request.purpose(), AgentTaskStatus.QUEUED, 0);
        return task;
    }

    private <T> CompletableFuture<AgentTaskResult<T>> execute(AgentTaskEntity task, Supplier<T> supplier) {
        CompletableFuture<AgentTaskResult<T>> future = CompletableFuture.supplyAsync(() -> runTask(task, supplier), executor);
        future.whenComplete((result, throwable) -> {
            if (throwable != null && task.getStatus() == AgentTaskStatus.RUNNING) {
                task.markFailed(safeError(throwable));
                updateTaskLifecycle(task);
                metrics.recordAgentTask(task.getPurpose(), AgentTaskStatus.FAILED, task.getLatencyMillis());
            }
        });
        return future;
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
