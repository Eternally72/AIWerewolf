package com.example.aiwerewolf.agent.core;

import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.game.view.PlayerView;
import com.example.aiwerewolf.role.model.Camp;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.room.entity.RoomStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentDecisionParserTest {
    private final AgentDecisionParser parser = new AgentDecisionParser(new ObjectMapper());

    @Test
    void rejectsInvalidJson() {
        assertThat(parser.parseSpeech("not-json")).isEmpty();
        assertThat(parser.hasJson("not-json")).isFalse();
    }

    @Test
    void rejectsWerewolfVotingVisibleTeammate() {
        GameView view = new GameView("room", "room", RoomStatus.RUNNING, GamePhase.DAY_VOTE, 1,
                "wolf1", Role.WEREWOLF, Camp.WEREWOLF,
                List.of(
                        new PlayerView("wolf1", 1, "W1", null, true, true, true, Role.WEREWOLF, Camp.WEREWOLF),
                        new PlayerView("wolf2", 2, "W2", null, true, true, true, Role.WEREWOLF, Camp.WEREWOLF),
                        new PlayerView("good", 3, "G", null, true, true, true, null, null)
                ),
                List.of(), List.of(), List.of(), false);

        assertThat(parser.parseVote("{\"targetPlayerId\":\"wolf2\",\"reason\":\"test\",\"confidence\":0.8}", "wolf1", view)).isEmpty();
        assertThat(parser.parseVote("{\"targetPlayerId\":\"good\",\"reason\":\"test\",\"confidence\":0.8}", "wolf1", view)).isPresent();
    }
}
