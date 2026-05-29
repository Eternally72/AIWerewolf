package com.example.aiwerewolf.agent.core;

import com.example.aiwerewolf.agent.dto.AiActionDecision;
import com.example.aiwerewolf.agent.dto.AiVoteDecision;
import com.example.aiwerewolf.agent.llm.MockLlmClient;
import com.example.aiwerewolf.agent.prompt.AgentPromptFactory;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.game.view.PlayerView;
import com.example.aiwerewolf.role.model.Camp;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.room.entity.RoomStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiAgentServiceTest {
    private final AiAgentService service = new AiAgentService(new MockLlmClient(), new AgentPromptFactory());

    @Test
    void aiUsesFilteredViewAndDoesNotNeedGodView() {
        GameView view = new GameView("r", "room", RoomStatus.RUNNING, GamePhase.DAY_VOTE, 1,
                "villager", Role.VILLAGER, Camp.GOOD,
                List.of(
                        new PlayerView("villager", 1, "P1", null, true, true, true, Role.VILLAGER, Camp.GOOD),
                        new PlayerView("unknown", 2, "P2", null, true, true, true, null, null)
                ),
                List.of(), List.of(), List.of(), false);

        AiVoteDecision vote = service.decideVote("villager", view);

        assertThat(view.godView()).isFalse();
        assertThat(vote.targetPlayerId()).isEqualTo("unknown");
    }

    @Test
    void mockNightActionFallsBackToLegalAction() {
        GameView view = new GameView("r", "room", RoomStatus.RUNNING, GamePhase.WEREWOLF_ACTION, 1,
                "wolf", Role.WEREWOLF, Camp.WEREWOLF,
                List.of(
                        new PlayerView("wolf", 1, "W", null, true, true, true, Role.WEREWOLF, Camp.WEREWOLF),
                        new PlayerView("good", 2, "G", null, true, true, true, null, null)
                ),
                List.of(), List.of(), List.of(), false);

        AiActionDecision action = service.decideNightAction("wolf", view);

        assertThat(action.targetPlayerId()).isEqualTo("good");
    }
}
