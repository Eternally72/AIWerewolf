package com.example.aiwerewolf.aiinfra.eval;

import java.time.Instant;
import java.util.List;

public record EvaluationReport(
        String evaluationId,
        String templateId,
        int requestedGames,
        int completedGames,
        int failedGames,
        double completionRate,
        int totalAgentRuns,
        int totalFallbackRuns,
        double fallbackRate,
        int invalidDecisionFallbackCount,
        int leakageCount,
        double averageLatencyMillis,
        double averageRounds,
        long totalDurationMillis,
        Instant createdAt,
        List<EvaluationGameResult> games
) {
}
