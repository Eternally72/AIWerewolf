package com.example.aiwerewolf.game.engine;

import com.example.aiwerewolf.action.service.DeathResolutionService;
import com.example.aiwerewolf.action.service.NightActionService;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.rule.VictoryConditionService;
import com.example.aiwerewolf.memory.service.MemoryService;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.room.repository.RoomRepository;
import com.example.aiwerewolf.speech.service.SpeechService;
import com.example.aiwerewolf.vote.service.VoteService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GamePhaseEngineTest {
    @Test
    void phaseTransitionOrderIsExplicit() {
        GamePhaseEngine engine = new GamePhaseEngine(
                mock(RoomRepository.class),
                mock(PlayerRepository.class),
                mock(NightActionService.class),
                mock(SpeechService.class),
                mock(VoteService.class),
                mock(DeathResolutionService.class),
                new VictoryConditionService(),
                mock(MemoryService.class)
        );

        assertThat(engine.validatePhaseTransition(GamePhase.FIRST_NIGHT, GamePhase.GUARD_ACTION)).isTrue();
        assertThat(engine.validatePhaseTransition(GamePhase.DAY_VOTE, GamePhase.EXECUTION)).isTrue();
        assertThat(engine.validatePhaseTransition(GamePhase.DAY_VOTE, GamePhase.NIGHT)).isFalse();
    }
}
