package com.example.aiwerewolf.agent.core;

import com.example.aiwerewolf.action.entity.ActionType;
import com.example.aiwerewolf.agent.dto.AiActionDecision;
import com.example.aiwerewolf.agent.dto.AiSpeechDecision;
import com.example.aiwerewolf.agent.dto.AiVoteDecision;
import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.game.view.PlayerView;
import com.example.aiwerewolf.role.model.Role;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class AgentFallbackStrategy {
    public AiSpeechDecision speech(GameView privateGameView) {
        Role role = privateGameView.ownRole();
        String claim = role != null && role.isWerewolfCamp() ? "好人" : role == null ? "" : role.displayName();
        return new AiSpeechDecision("我先盘公开信息：重点看昨夜死亡和今天发言前后是否矛盾。我的当前倾向是稳票，不急着暴露身份。", claim, "保守发言，观察票型。");
    }

    public AiVoteDecision vote(String agentId, GameView privateGameView) {
        String target = privateGameView.players().stream()
                .filter(PlayerView::alive)
                .filter(player -> !player.id().equals(agentId))
                .filter(player -> !sameWerewolfTeam(privateGameView.ownRole(), player.role()))
                .map(PlayerView::id)
                .findFirst()
                .orElse(agentId);
        return new AiVoteDecision(target, "Mock AI 根据公开信息选择当前最可疑目标", 0.55);
    }

    public AiActionDecision nightAction(String agentId, GameView privateGameView) {
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

    public AiActionDecision daySkill() {
        return new AiActionDecision(ActionType.NONE, null, null, "暂不发动白天技能");
    }

    @Nullable
    private String firstOtherAlive(String agentId, GameView view) {
        return view.players().stream()
                .filter(PlayerView::alive)
                .filter(player -> !player.id().equals(agentId))
                .map(PlayerView::id)
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private String firstGoodAlive(String agentId, GameView view) {
        return view.players().stream()
                .filter(PlayerView::alive)
                .filter(player -> !player.id().equals(agentId))
                .filter(player -> {
                    Role visibleRole = player.role();
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
