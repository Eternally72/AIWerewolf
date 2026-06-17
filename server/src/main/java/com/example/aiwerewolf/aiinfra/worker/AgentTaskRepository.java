package com.example.aiwerewolf.aiinfra.worker;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface AgentTaskRepository extends JpaRepository<AgentTaskEntity, String> {
    List<AgentTaskEntity> findTop100ByRoomIdOrderByQueuedAtDesc(String roomId);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AgentTaskEntity task
               set task.status = :status,
                   task.startedAt = :startedAt,
                   task.completedAt = :completedAt,
                   task.latencyMillis = :latencyMillis,
                   task.errorMessage = :errorMessage,
                   task.updatedAt = :updatedAt
             where task.id = :id
            """)
    int updateLifecycle(String id,
                        AgentTaskStatus status,
                        @Nullable Instant startedAt,
                        @Nullable Instant completedAt,
                        long latencyMillis,
                        @Nullable String errorMessage,
                        Instant updatedAt);
}
