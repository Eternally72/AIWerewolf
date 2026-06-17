package com.example.aiwerewolf.aiinfra.gateway;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MockModelProvider implements ModelProvider {
    private static final Pattern AGENT_ID = Pattern.compile("agentId=([^\\n\\r]+)");
    private static final Pattern PLAYER_ID = Pattern.compile("PlayerView\\[id=([^,]+)");
    private static final Pattern OWN_ROLE = Pattern.compile("ownRole=([^,\\)]+)");

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public String modelName() {
        return "mock-json-v1";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        String agentId = extract(AGENT_ID, userPrompt, "mock-agent");
        String role = extract(OWN_ROLE, userPrompt, "UNKNOWN");
        List<String> targets = targets(userPrompt, agentId);
        String target = targets.isEmpty() ? "" : targets.get(Math.floorMod(agentId.hashCode(), targets.size()));
        int variant = Math.floorMod((agentId + role).hashCode(), 6);

        if (userPrompt.contains("targetPlayerId") && userPrompt.contains("confidence")) {
            return """
                    {"targetPlayerId":"%s","reason":"我根据发言强度、存活位置和上一轮信息选择当前更可疑的目标。","confidence":0.%d}
                    """.formatted(escape(target), 55 + variant);
        }
        if (userPrompt.contains("secondaryTargetPlayerId") || userPrompt.contains("actionType")) {
            String action = nightAction(role);
            String actionTarget = "NONE".equals(action) ? "" : target;
            return """
                    {"actionType":"%s","targetPlayerId":"%s","secondaryTargetPlayerId":null,"reason":"Mock Agent 根据角色能力和当前可见信息选择行动。"}
                    """.formatted(action, escape(actionTarget));
        }
        return """
                {"speech":"%s","claimedRole":null,"suspicions":[],"trustedPlayers":[],"strategySummary":"Mock 策略：%s"}
                """.formatted(escape(speech(role, variant)), escape(strategy(role, variant)));
    }

    private String speech(String role, int variant) {
        return switch (variant) {
            case 0 -> "我先保留判断。现在信息还不够，重点看谁在跟票和谁在回避死亡信息。";
            case 1 -> "我更关注发言顺序里的压力变化。有人在刻意把讨论带离关键票型。";
            case 2 -> role.contains("WOLF") ? "我站在好人视角聊，今天先找发言前后矛盾的人，不急着定死身份。" : "我倾向从票型和夜里死亡位置倒推狼队收益。";
            case 3 -> "目前我不认同盲目归票。先让高争议位置多解释，再决定投票方向。";
            case 4 -> "我会把注意力放在低互动玩家身上，安静不代表做好身份。";
            default -> "这一轮我先听后置位补充，暂时不跳身份，也不跟着情绪投票。";
        };
    }

    private String strategy(String role, int variant) {
        return switch (variant) {
            case 0 -> "观察跟票关系，避免过早暴露底牌。";
            case 1 -> "制造或识别压力位，记录可疑发言。";
            case 2 -> role.contains("WOLF") ? "伪装成谨慎好人，降低团队关联。" : "从死亡收益倒推狼坑。";
            case 3 -> "延迟站边，等待更多公开信息。";
            case 4 -> "关注低互动玩家和异常沉默。";
            default -> "保持弹性发言，等待投票阶段。";
        };
    }

    private String nightAction(String role) {
        if (role.contains("SEER")) {
            return "CHECK";
        }
        if (role.contains("WITCH")) {
            return "NONE";
        }
        if (role.contains("GUARD")) {
            return "GUARD";
        }
        if (role.contains("WOLF")) {
            return "KILL";
        }
        return "NONE";
    }

    private List<String> targets(String prompt, String agentId) {
        Matcher matcher = PLAYER_ID.matcher(prompt);
        List<String> ids = new ArrayList<>();
        while (matcher.find()) {
            String id = matcher.group(1).strip();
            if (!id.equals(agentId) && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private String extract(Pattern pattern, String value, String fallback) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1).strip() : fallback;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
