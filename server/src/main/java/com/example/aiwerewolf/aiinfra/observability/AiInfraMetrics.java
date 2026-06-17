package com.example.aiwerewolf.aiinfra.observability;

import com.example.aiwerewolf.aiinfra.run.AgentRunPurpose;
import com.example.aiwerewolf.aiinfra.run.AgentRunStatus;
import com.example.aiwerewolf.aiinfra.worker.AgentTaskStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Service
public class AiInfraMetrics {
    private static final String UNKNOWN = "unknown";

    private final MeterRegistry meterRegistry;

    public AiInfraMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordLlmCall(@Nullable AgentRunPurpose purpose,
                              @Nullable String provider,
                              @Nullable String model,
                              boolean fallbackUsed,
                              int attemptCount,
                              long latencyMillis) {
        Tags tags = Tags.of(
                "purpose", purposeTag(purpose),
                "provider", tagValue(provider),
                "model", tagValue(model),
                "fallback", Boolean.toString(fallbackUsed));
        Counter.builder("aiwerewolf.llm.calls")
                .description("Total LLM gateway calls")
                .tags(tags)
                .register(meterRegistry)
                .increment();
        if (fallbackUsed) {
            Counter.builder("aiwerewolf.llm.fallbacks")
                    .description("Total LLM gateway calls served by fallback provider")
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        }
        DistributionSummary.builder("aiwerewolf.llm.attempts")
                .description("Provider attempts per LLM gateway call")
                .tags(tags)
                .register(meterRegistry)
                .record(Math.max(0, attemptCount));
        Timer.builder("aiwerewolf.llm.latency")
                .description("LLM gateway latency")
                .tags(tags)
                .register(meterRegistry)
                .record(duration(latencyMillis));
    }

    public void recordAgentRun(@Nullable AgentRunPurpose purpose,
                               @Nullable AgentRunStatus status,
                               boolean fallbackUsed,
                               long latencyMillis) {
        Tags tags = Tags.of(
                "purpose", purposeTag(purpose),
                "status", status == null ? UNKNOWN : status.name().toLowerCase(Locale.ROOT),
                "fallback", Boolean.toString(fallbackUsed));
        Counter.builder("aiwerewolf.agent.runs")
                .description("Persisted AI agent decision runs")
                .tags(tags)
                .register(meterRegistry)
                .increment();
        if (fallbackUsed) {
            Counter.builder("aiwerewolf.agent.fallbacks")
                    .description("Persisted AI agent decision runs that used fallback")
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        }
        Timer.builder("aiwerewolf.agent.run.latency")
                .description("AI agent decision run latency")
                .tags(tags)
                .register(meterRegistry)
                .record(duration(latencyMillis));
    }

    public void recordAgentTask(@Nullable AgentRunPurpose purpose,
                                @Nullable AgentTaskStatus status,
                                long latencyMillis) {
        Tags tags = Tags.of(
                "purpose", purposeTag(purpose),
                "status", status == null ? UNKNOWN : status.name().toLowerCase(Locale.ROOT));
        Counter.builder("aiwerewolf.agent.tasks")
                .description("AI agent worker task state transitions")
                .tags(tags)
                .register(meterRegistry)
                .increment();
        if (status == AgentTaskStatus.SUCCEEDED || status == AgentTaskStatus.FAILED || status == AgentTaskStatus.TIMED_OUT) {
            Timer.builder("aiwerewolf.agent.task.latency")
                    .description("AI agent worker task execution latency")
                    .tags(tags)
                    .register(meterRegistry)
                    .record(duration(latencyMillis));
        }
    }

    public void recordPhaseAdvance(@Nullable String phase, String status, long latencyMillis) {
        Tags tags = Tags.of(
                "phase", tagValue(phase),
                "status", tagValue(status));
        Counter.builder("aiwerewolf.game.phase.advances")
                .description("Game phase advance attempts")
                .tags(tags)
                .register(meterRegistry)
                .increment();
        Timer.builder("aiwerewolf.game.phase.advance.latency")
                .description("Game phase advance latency")
                .tags(tags)
                .register(meterRegistry)
                .record(duration(latencyMillis));
    }

    public void recordEvaluation(String templateId,
                                 int completedGames,
                                 int failedGames,
                                 int leakageCount,
                                 double fallbackRate,
                                 long durationMillis) {
        Tags tags = Tags.of("template", tagValue(templateId));
        Counter.builder("aiwerewolf.evaluation.runs")
                .description("Evaluation suite executions")
                .tags(tags)
                .register(meterRegistry)
                .increment();
        Counter.builder("aiwerewolf.evaluation.completed.games")
                .description("Completed evaluation games")
                .tags(tags)
                .register(meterRegistry)
                .increment(Math.max(0, completedGames));
        Counter.builder("aiwerewolf.evaluation.failed.games")
                .description("Failed evaluation games")
                .tags(tags)
                .register(meterRegistry)
                .increment(Math.max(0, failedGames));
        Counter.builder("aiwerewolf.evaluation.leakage")
                .description("Detected public-view information leakage during evaluation")
                .tags(tags)
                .register(meterRegistry)
                .increment(Math.max(0, leakageCount));
        DistributionSummary.builder("aiwerewolf.evaluation.fallback.rate")
                .description("Fallback rate per evaluation suite")
                .tags(tags)
                .register(meterRegistry)
                .record(Math.max(0.0, fallbackRate));
        Timer.builder("aiwerewolf.evaluation.duration")
                .description("Evaluation suite duration")
                .tags(tags)
                .register(meterRegistry)
                .record(duration(durationMillis));
    }

    private Duration duration(long millis) {
        return Duration.ofMillis(Math.max(0, millis));
    }

    private String purposeTag(@Nullable AgentRunPurpose purpose) {
        return purpose == null ? UNKNOWN : purpose.name().toLowerCase(Locale.ROOT);
    }

    private String tagValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return value.strip().toLowerCase(Locale.ROOT);
    }
}
