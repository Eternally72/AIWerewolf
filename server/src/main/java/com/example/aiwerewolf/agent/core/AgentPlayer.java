package com.example.aiwerewolf.agent.core;

import com.example.aiwerewolf.agent.dto.AiActionDecision;
import com.example.aiwerewolf.agent.dto.AiSpeechDecision;
import com.example.aiwerewolf.agent.dto.AiVoteDecision;
import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.role.model.Camp;
import com.example.aiwerewolf.role.model.Role;

public interface AgentPlayer {
    String getPlayerId();
    Role getRole();
    Camp getCamp();
    AiSpeechDecision generateSpeech(GameView view);
    AiVoteDecision decideVote(GameView view);
    AiActionDecision decideNightAction(GameView view);
    AiActionDecision decideDaySkill(GameView view);
    default void onPrivateEvent(Object event) {
    }
    default void onPublicEvent(Object event) {
    }
}
