package com.example.aiwerewolf.agent.core;

import com.example.aiwerewolf.aiinfra.gateway.LlmGateway;
import com.example.aiwerewolf.aiinfra.gateway.ModelCallRequest;
import com.example.aiwerewolf.aiinfra.gateway.ModelCallResponse;
import com.example.aiwerewolf.aiinfra.prompt.PromptBundle;
import com.example.aiwerewolf.aiinfra.prompt.PromptRegistry;
import com.example.aiwerewolf.aiinfra.run.AgentRunPurpose;
import com.example.aiwerewolf.aiinfra.run.AgentRunRecord;
import com.example.aiwerewolf.aiinfra.run.AgentRunService;
import com.example.aiwerewolf.aiinfra.run.AgentRunStatus;
import com.example.aiwerewolf.agent.dto.AiActionDecision;
import com.example.aiwerewolf.agent.dto.AiSpeechDecision;
import com.example.aiwerewolf.agent.dto.AiVoteDecision;
import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.memory.service.AgentShortTermMemoryService;
import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;

@Service
public class AiAgentService {
    private final LlmGateway llmGateway;
    private final PromptRegistry promptRegistry;
    private final AgentShortTermMemoryService shortTermMemoryService;
    private final AgentRunService agentRunService;
    private final AgentDecisionParser decisionParser;
    private final AgentFallbackStrategy fallbackStrategy;

    public AiAgentService(LlmGateway llmGateway,
                          PromptRegistry promptRegistry,
                          AgentShortTermMemoryService shortTermMemoryService,
                          AgentRunService agentRunService,
                          AgentDecisionParser decisionParser,
                          AgentFallbackStrategy fallbackStrategy) {
        this.llmGateway = llmGateway;
        this.promptRegistry = promptRegistry;
        this.shortTermMemoryService = shortTermMemoryService;
        this.agentRunService = agentRunService;
        this.decisionParser = decisionParser;
        this.fallbackStrategy = fallbackStrategy;
    }

    public AiSpeechDecision generateSpeech(String agentId, GameView privateGameView) {
        long startedAt = System.nanoTime();
        AgentCallResult call = callAgent(agentId, privateGameView, AgentRunPurpose.SPEECH);
        java.util.Optional<AiSpeechDecision> parsed = decisionParser.parseSpeech(call.rawOutput());
        boolean fallbackUsed = parsed.isEmpty();
        AiSpeechDecision decision = parsed.orElseGet(() -> fallbackStrategy.speech(privateGameView));
        recordRun(agentId, privateGameView, AgentRunPurpose.SPEECH, call, decision, fallbackUsed, startedAt,
                mergeError(call.errorMessage(), fallbackUsed ? "模型发言输出为空或格式非法，已使用 fallback 发言" : null));
        return decision;
    }

    public AiVoteDecision decideVote(String agentId, GameView privateGameView) {
        long startedAt = System.nanoTime();
        AgentCallResult call = callAgent(agentId, privateGameView, AgentRunPurpose.VOTE);
        java.util.Optional<AiVoteDecision> parsed = decisionParser.parseVote(call.rawOutput(), agentId, privateGameView);
        boolean fallbackUsed = parsed.isEmpty();
        AiVoteDecision decision = parsed.orElseGet(() -> fallbackStrategy.vote(agentId, privateGameView));
        recordRun(agentId, privateGameView, AgentRunPurpose.VOTE, call, decision, fallbackUsed, startedAt,
                mergeError(call.errorMessage(), fallbackUsed ? "模型投票输出为空、格式非法或目标非法，已使用 fallback 投票" : null));
        return decision;
    }

    public AiActionDecision decideNightAction(String agentId, GameView privateGameView) {
        long startedAt = System.nanoTime();
        AgentCallResult call = callAgent(agentId, privateGameView, AgentRunPurpose.NIGHT_ACTION);
        java.util.Optional<AiActionDecision> parsed = decisionParser.parseAction(call.rawOutput(), agentId, privateGameView);
        boolean fallbackUsed = parsed.isEmpty();
        AiActionDecision decision = parsed.orElseGet(() -> fallbackStrategy.nightAction(agentId, privateGameView));
        recordRun(agentId, privateGameView, AgentRunPurpose.NIGHT_ACTION, call, decision, fallbackUsed, startedAt,
                mergeError(call.errorMessage(), fallbackUsed ? "模型夜间行动输出为空、格式非法或目标非法，已使用 fallback 行动" : null));
        return decision;
    }

    public AiActionDecision decideDaySkill(String agentId, GameView privateGameView) {
        long startedAt = System.nanoTime();
        AgentCallResult call = callAgent(agentId, privateGameView, AgentRunPurpose.DAY_SKILL);
        java.util.Optional<AiActionDecision> parsed = decisionParser.parseAction(call.rawOutput(), agentId, privateGameView);
        boolean fallbackUsed = parsed.isEmpty();
        AiActionDecision decision = parsed.orElseGet(fallbackStrategy::daySkill);
        recordRun(agentId, privateGameView, AgentRunPurpose.DAY_SKILL, call, decision, fallbackUsed, startedAt,
                mergeError(call.errorMessage(), fallbackUsed ? "模型白天技能输出为空、格式非法或目标非法，已使用 fallback 行动" : null));
        return decision;
    }

    private AgentCallResult callAgent(String agentId, GameView privateGameView, AgentRunPurpose purpose) {
        String memory = String.join("\n", shortTermMemoryService.listRecent(privateGameView.roomId(), agentId));
        PromptBundle prompt = promptRegistry.buildAgentPrompt(agentId, memory, privateGameView, purpose);
        String lastResponse = "";
        String providerName = "";
        String modelName = "";
        String errorMessage = null;
        boolean providerFallbackUsed = false;
        int providerAttemptCount = 0;
        long modelLatencyMillis = 0;
        for (int attempt = 0; attempt < 2; attempt++) {
            ModelCallResponse response = llmGateway.complete(new ModelCallRequest(
                    privateGameView.roomId(), agentId, purpose, prompt.systemPrompt(), prompt.userPrompt()));
            providerName = response.providerName();
            modelName = response.modelName();
            errorMessage = response.errorMessage();
            providerFallbackUsed = providerFallbackUsed || response.providerFallbackUsed();
            providerAttemptCount += response.providerAttemptCount();
            modelLatencyMillis += response.latencyMillis();
            lastResponse = response.content();
            if (decisionParser.hasJson(lastResponse)) {
                return new AgentCallResult(lastResponse, providerName, modelName,
                        providerFallbackUsed, providerAttemptCount, modelLatencyMillis, errorMessage,
                        prompt.rolePromptVersion(), prompt.taskPromptVersion(), prompt.outputSchemaVersion());
            }
        }
        return new AgentCallResult(lastResponse, providerName, modelName,
                providerFallbackUsed, providerAttemptCount, modelLatencyMillis, errorMessage,
                prompt.rolePromptVersion(), prompt.taskPromptVersion(), prompt.outputSchemaVersion());
    }

    private void recordRun(String agentId,
                           GameView privateGameView,
                           AgentRunPurpose purpose,
                           AgentCallResult call,
                           Object parsedOutput,
                           boolean fallbackUsed,
                           long startedAt,
                           @Nullable String errorMessage) {
        long latencyMillis = Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
        boolean anyFallbackUsed = fallbackUsed || call.providerFallbackUsed();
        agentRunService.record(new AgentRunRecord(
                privateGameView.roomId(),
                agentId,
                agentId,
                privateGameView.roundNumber(),
                privateGameView.phase(),
                purpose,
                anyFallbackUsed ? AgentRunStatus.FALLBACK : AgentRunStatus.SUCCESS,
                anyFallbackUsed,
                call.providerAttemptCount(),
                Math.max(latencyMillis, call.modelLatencyMillis()),
                call.rolePromptVersion(),
                call.taskPromptVersion() + "/" + call.outputSchemaVersion(),
                blankToDefault(call.providerName(), "unknown"),
                blankToNull(call.modelName()),
                privateGameView,
                call.rawOutput(),
                parsedOutput,
                errorMessage));
    }

    @Nullable
    private String mergeError(@Nullable String gatewayError, @Nullable String parseError) {
        if (gatewayError == null || gatewayError.isBlank()) {
            return parseError;
        }
        if (parseError == null || parseError.isBlank()) {
            return gatewayError;
        }
        return gatewayError + "; " + parseError;
    }

    @Nullable
    private String blankToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private String blankToDefault(@Nullable String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private record AgentCallResult(
            String rawOutput,
            String providerName,
            String modelName,
            boolean providerFallbackUsed,
            int providerAttemptCount,
            long modelLatencyMillis,
            @Nullable String errorMessage,
            String rolePromptVersion,
            String taskPromptVersion,
            String outputSchemaVersion
    ) {
    }
}
