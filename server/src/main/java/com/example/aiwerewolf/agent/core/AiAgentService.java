package com.example.aiwerewolf.agent.core;

import com.example.aiwerewolf.action.entity.ActionType;
import com.example.aiwerewolf.agent.dto.AiActionDecision;
import com.example.aiwerewolf.agent.dto.AiSpeechDecision;
import com.example.aiwerewolf.agent.dto.AiVoteDecision;
import com.example.aiwerewolf.agent.llm.LlmClient;
import com.example.aiwerewolf.agent.prompt.AgentPromptFactory;
import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.game.view.PlayerView;
import com.example.aiwerewolf.memory.service.AgentShortTermMemoryService;
import com.example.aiwerewolf.role.model.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;

@Service
public class AiAgentService {
    private final LlmClient llmClient;
    private final AgentPromptFactory promptFactory;
    private final ObjectMapper objectMapper;
    private final AgentShortTermMemoryService shortTermMemoryService;

    public AiAgentService(LlmClient llmClient,
                          AgentPromptFactory promptFactory,
                          ObjectMapper objectMapper,
                          AgentShortTermMemoryService shortTermMemoryService) {
        this.llmClient = llmClient;
        this.promptFactory = promptFactory;
        this.objectMapper = objectMapper;
        this.shortTermMemoryService = shortTermMemoryService;
    }

    public AiSpeechDecision generateSpeech(String agentId, GameView privateGameView) {
        return parseSpeech(callAgent(agentId, privateGameView, """
                请严格输出一个 JSON 对象：
                {"speech":"公开发言","claimedRole":"声称身份，可为空","suspicions":[],"trustedPlayers":[],"strategySummary":"仅系统可见的策略摘要"}
                不要输出 Markdown，不要解释 JSON。
                """)).orElseGet(() -> fallbackSpeech(privateGameView));
    }

    public AiVoteDecision decideVote(String agentId, GameView privateGameView) {
        return parseVote(callAgent(agentId, privateGameView, """
                请严格输出一个 JSON 对象：
                {"targetPlayerId":"投票目标玩家ID","reason":"投票理由","confidence":0.7}
                targetPlayerId 必须来自当前视角中的存活玩家，不能投自己，狼人尽量不要投可见狼队友。
                """), agentId, privateGameView).orElseGet(() -> fallbackVote(agentId, privateGameView));
    }

    public AiActionDecision decideNightAction(String agentId, GameView privateGameView) {
        return parseAction(callAgent(agentId, privateGameView, """
                请严格输出一个 JSON 对象：
                {"actionType":"KILL / CHECK / SAVE / POISON / GUARD / SWAP / LINK_LOVERS / NONE","targetPlayerId":"主要目标，可为空","secondaryTargetPlayerId":"第二目标，可为空","reason":"行动理由"}
                目标必须来自当前视角中的存活玩家。没有合法行动时 actionType 使用 NONE。
                """), agentId, privateGameView).orElseGet(() -> fallbackNightAction(agentId, privateGameView));
    }

    public AiActionDecision decideDaySkill(String agentId, GameView privateGameView) {
        return parseAction(callAgent(agentId, privateGameView, """
                请严格输出一个 JSON 对象：
                {"actionType":"DUEL / EXPLODE_AND_KILL / SHOOT / REVEAL_IDIOT / NONE","targetPlayerId":"目标玩家ID，可为空","secondaryTargetPlayerId":null,"reason":"行动理由"}
                没有把握时 actionType 使用 NONE。
                """), agentId, privateGameView).orElseGet(() -> new AiActionDecision(ActionType.NONE, null, null, "暂不发动白天技能"));
    }

    private String callAgent(String agentId, GameView privateGameView, String task) {
        String systemPrompt = promptFactory.buildSystemPrompt(privateGameView.ownRole());
        String memory = String.join("\n", shortTermMemoryService.listRecent(privateGameView.roomId(), agentId));
        String userPrompt = """
                agentId=%s
                短期记忆：
                %s

                当前过滤后的私有视角：
                %s

                任务：
                %s
                """.formatted(agentId, memory, privateGameView, task);
        for (int attempt = 0; attempt < 2; attempt++) {
            String response = llmClient.complete(systemPrompt, userPrompt);
            if (extractJson(response) != null) {
                return response;
            }
        }
        return "";
    }

    private java.util.Optional<AiSpeechDecision> parseSpeech(String response) {
        JsonNode json = readJson(response);
        if (json == null) {
            return java.util.Optional.empty();
        }
        String speech = text(json, "speech");
        if (speech == null || speech.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new AiSpeechDecision(
                speech.strip(),
                blankToEmpty(text(json, "claimedRole")),
                blankToDefault(text(json, "strategySummary"), "模型未提供策略摘要")));
    }

    private java.util.Optional<AiVoteDecision> parseVote(String response, String agentId, GameView view) {
        JsonNode json = readJson(response);
        if (json == null) {
            return java.util.Optional.empty();
        }
        String targetId = text(json, "targetPlayerId");
        if (!validVoteTarget(agentId, view, targetId)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new AiVoteDecision(
                targetId,
                blankToDefault(text(json, "reason"), "根据当前可见信息投票"),
                confidence(json)));
    }

    private java.util.Optional<AiActionDecision> parseAction(String response, String agentId, GameView view) {
        JsonNode json = readJson(response);
        if (json == null) {
            return java.util.Optional.empty();
        }
        ActionType actionType = parseActionType(text(json, "actionType"));
        if (actionType == null) {
            return java.util.Optional.empty();
        }
        String targetId = blankToNull(text(json, "targetPlayerId"));
        String secondaryTargetId = blankToNull(text(json, "secondaryTargetPlayerId"));
        if (requiresTarget(actionType) && !validActionTarget(agentId, view, targetId)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new AiActionDecision(
                actionType,
                targetId,
                secondaryTargetId,
                blankToDefault(text(json, "reason"), "根据当前可见信息行动")));
    }

    private AiSpeechDecision fallbackSpeech(GameView privateGameView) {
        Role role = privateGameView.ownRole();
        String claim = role != null && role.isWerewolfCamp() ? "好人" : role == null ? "" : role.displayName();
        return new AiSpeechDecision("我先盘公开信息：重点看昨夜死亡和今天发言前后是否矛盾。我的当前倾向是稳票，不急着暴露身份。", claim, "保守发言，观察票型。");
    }

    private AiVoteDecision fallbackVote(String agentId, GameView privateGameView) {
        String target = privateGameView.players().stream()
                .filter(PlayerView::alive)
                .filter(p -> !p.id().equals(agentId))
                .filter(p -> !sameWerewolfTeam(privateGameView.ownRole(), p.role()))
                .map(PlayerView::id)
                .findFirst()
                .orElse(agentId);
        return new AiVoteDecision(target, "Mock AI 根据公开信息选择当前最可疑目标", 0.55);
    }

    private AiActionDecision fallbackNightAction(String agentId, GameView privateGameView) {
        Role role = privateGameView.ownRole();
        if (role == Role.SEER) {
            return new AiActionDecision(ActionType.CHECK, firstOtherAlive(agentId, privateGameView), null, "查验可疑玩家");
        }
        if (role == Role.WITCH) {
            return new AiActionDecision(ActionType.NONE, null, null, "保留药品等待更明确局势");
        }
        if (role == Role.GUARD) {
            return new AiActionDecision(ActionType.GUARD, firstOtherAlive(agentId, privateGameView), null, "守护高价值目标");
        }
        if (role != null && role.isWerewolfCamp()) {
            return new AiActionDecision(ActionType.KILL, firstGoodAlive(agentId, privateGameView), null, "优先刀疑似好人核心");
        }
        return new AiActionDecision(ActionType.NONE, null, null, "无夜间行动");
    }

    @Nullable
    private JsonNode readJson(String response) {
        String json = extractJson(response);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return null;
        }
    }

    @Nullable
    private String extractJson(@Nullable String response) {
        if (response == null || response.isBlank()) {
            return null;
        }
        String text = response.strip();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return text.substring(start, end + 1);
    }

    @Nullable
    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }

    private double confidence(JsonNode node) {
        JsonNode value = node.get("confidence");
        if (value == null || !value.isNumber()) {
            return 0.5;
        }
        double confidence = value.asDouble();
        return Math.max(0.0, Math.min(1.0, confidence));
    }

    @Nullable
    private ActionType parseActionType(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ActionType.valueOf(value.strip().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean validVoteTarget(String agentId, GameView view, @Nullable String targetId) {
        if (!validActionTarget(agentId, view, targetId)) {
            return false;
        }
        Role ownRole = view.ownRole();
        return view.players().stream()
                .filter(player -> player.id().equals(targetId))
                .findFirst()
                .map(player -> !sameWerewolfTeam(ownRole, player.role()))
                .orElse(false);
    }

    private boolean validActionTarget(String agentId, GameView view, @Nullable String targetId) {
        if (targetId == null || targetId.isBlank() || targetId.equals(agentId)) {
            return false;
        }
        return view.players().stream().anyMatch(player -> player.id().equals(targetId) && player.alive());
    }

    private boolean requiresTarget(ActionType type) {
        return switch (type) {
            case KILL, CHECK, POISON, GUARD, SHOOT, DUEL, EXPLODE_AND_KILL -> true;
            default -> false;
        };
    }

    private String blankToEmpty(@Nullable String value) {
        return value == null ? "" : value.strip();
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

    @Nullable
    private String firstOtherAlive(String agentId, GameView view) {
        return view.players().stream()
                .filter(PlayerView::alive)
                .filter(p -> !p.id().equals(agentId))
                .map(PlayerView::id)
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private String firstGoodAlive(String agentId, GameView view) {
        return view.players().stream()
                .filter(PlayerView::alive)
                .filter(p -> !p.id().equals(agentId))
                .filter(p -> {
                    Role visibleRole = p.role();
                    return visibleRole == null || !visibleRole.isWerewolfCamp();
                })
                .map(PlayerView::id)
                .findFirst()
                .orElse(firstOtherAlive(agentId, view));
    }

    private boolean sameWerewolfTeam(@Nullable Role ownRole, @Nullable Role visibleRole) {
        return ownRole != null && visibleRole != null && ownRole.isWerewolfCamp() && visibleRole.isWerewolfCamp();
    }
}
