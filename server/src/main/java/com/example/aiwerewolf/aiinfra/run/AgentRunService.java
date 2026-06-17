package com.example.aiwerewolf.aiinfra.run;

import com.example.aiwerewolf.aiinfra.observability.AiInfraMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AgentRunService {
    private static final Logger log = LoggerFactory.getLogger(AgentRunService.class);

    private final AgentRunRepository repository;
    private final ObjectMapper objectMapper;
    private final AiInfraMetrics metrics;

    public AgentRunService(AgentRunRepository repository, ObjectMapper objectMapper, AiInfraMetrics metrics) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    @Transactional
    public void record(AgentRunRecord record) {
        try {
            AgentRunEntity entity = new AgentRunEntity();
            entity.setRoomId(record.roomId());
            entity.setPlayerId(record.playerId());
            entity.setAgentId(record.agentId());
            entity.setRoundNumber(record.roundNumber());
            entity.setPhase(record.phase());
            entity.setPurpose(record.purpose());
            entity.setStatus(record.status());
            entity.setFallbackUsed(record.fallbackUsed());
            entity.setAttemptCount(record.attemptCount());
            entity.setLatencyMillis(record.latencyMillis());
            entity.setPromptVersion(record.promptVersion());
            entity.setTaskPromptVersion(record.taskPromptVersion());
            entity.setModelProvider(record.modelProvider());
            entity.setModelName(record.modelName());
            entity.setInputViewSnapshotJson(toJson(record.inputViewSnapshot(), "{}"));
            entity.setRawOutput(blankToNull(record.rawOutput()));
            entity.setParsedOutputJson(toJson(record.parsedOutput(), null));
            entity.setErrorMessage(truncate(record.errorMessage(), 500));
            repository.save(entity);
            metrics.recordAgentRun(record.purpose(), record.status(), record.fallbackUsed(), record.latencyMillis());
        } catch (Exception ex) {
            log.warn("Failed to record agent run roomId={} agentId={} purpose={}",
                    record.roomId(), record.agentId(), record.purpose(), ex);
        }
    }

    @Transactional(readOnly = true)
    public List<AgentRunResponse> listRecentForRoom(String roomId) {
        return repository.findTop100ByRoomIdOrderByCreatedAtDesc(roomId).stream()
                .map(AgentRunResponse::fromEntity)
                .toList();
    }

    @Nullable
    private String toJson(@Nullable Object value, @Nullable String fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return fallback;
        }
    }

    @Nullable
    private String blankToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    @Nullable
    private String truncate(@Nullable String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
