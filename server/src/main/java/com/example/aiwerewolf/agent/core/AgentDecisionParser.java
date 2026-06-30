package com.example.aiwerewolf.agent.core;

import com.example.aiwerewolf.action.entity.ActionType;
import com.example.aiwerewolf.agent.dto.AiActionDecision;
import com.example.aiwerewolf.agent.dto.AiSpeechDecision;
import com.example.aiwerewolf.agent.dto.AiVoteDecision;
import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.role.model.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AgentDecisionParser {
    private final ObjectMapper objectMapper;

    public AgentDecisionParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<AiSpeechDecision> parseSpeech(String response) {
        JsonNode json = readJson(response);
        if (json == null) {
            return Optional.empty();
        }
        String speech = text(json, "speech");
        if (speech == null || speech.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new AiSpeechDecision(
                speech.strip(),
                blankToEmpty(text(json, "claimedRole")),
                blankToDefault(text(json, "strategySummary"), "模型未提供策略摘要")));
    }

    public Optional<AiVoteDecision> parseVote(String response, String agentId, GameView view) {
        JsonNode json = readJson(response);
        if (json == null) {
            return Optional.empty();
        }
        String targetId = resolvePlayerId(view, firstText(json, "targetPlayerRef", "targetPlayerId"));
        if (!validVoteTarget(agentId, view, targetId)) {
            return Optional.empty();
        }
        return Optional.of(new AiVoteDecision(
                targetId,
                blankToDefault(text(json, "reason"), "根据当前可见信息投票"),
                confidence(json)));
    }

    public Optional<AiActionDecision> parseAction(String response, String agentId, GameView view) {
        JsonNode json = readJson(response);
        if (json == null) {
            return Optional.empty();
        }
        ActionType actionType = parseActionType(text(json, "actionType"));
        if (actionType == null) {
            return Optional.empty();
        }
        String targetId = resolvePlayerId(view, firstText(json, "targetPlayerRef", "targetPlayerId"));
        String secondaryTargetId = resolvePlayerId(view,
                firstText(json, "secondaryTargetPlayerRef", "secondaryTargetPlayerId"));
        if (requiresTarget(actionType) && !validActionTarget(agentId, view, targetId)) {
            return Optional.empty();
        }
        return Optional.of(new AiActionDecision(
                actionType,
                targetId,
                secondaryTargetId,
                blankToDefault(text(json, "reason"), "根据当前可见信息行动")));
    }

    public boolean hasJson(@Nullable String response) {
        return extractJson(response) != null;
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

    @Nullable
    private String firstText(JsonNode node, String primaryField, String compatibilityField) {
        String value = text(node, primaryField);
        return value == null || value.isBlank() ? text(node, compatibilityField) : value;
    }

    @Nullable
    private String resolvePlayerId(GameView view, @Nullable String playerRef) {
        if (playerRef == null || playerRef.isBlank()) {
            return null;
        }
        String normalized = playerRef.strip().toLowerCase();
        return view.players().stream()
                .filter(player -> player.id().equals(playerRef.strip())
                        || normalized.equals("seat-" + player.seatNumber())
                        || normalized.equals(String.valueOf(player.seatNumber()))
                        || normalized.equals(player.seatNumber() + "号"))
                .map(player -> player.id())
                .findFirst()
                .orElse(null);
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

    private boolean sameWerewolfTeam(@Nullable Role ownRole, @Nullable Role visibleRole) {
        return ownRole != null && visibleRole != null && ownRole.isWerewolfCamp() && visibleRole.isWerewolfCamp();
    }
}
