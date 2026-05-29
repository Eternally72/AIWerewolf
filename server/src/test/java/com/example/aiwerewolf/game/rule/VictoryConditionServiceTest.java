package com.example.aiwerewolf.game.rule;

import com.example.aiwerewolf.TestFixtures;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.role.model.Camp;
import com.example.aiwerewolf.role.model.Role;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VictoryConditionServiceTest {
    private final VictoryConditionService service = new VictoryConditionService();

    @Test
    void allWolvesDeadGoodWins() {
        List<PlayerEntity> players = players();
        players.stream().filter(p -> p.getRole().isWerewolfCamp()).forEach(p -> p.setAlive(false));

        assertThat(service.checkVictory(players, VictoryRule.SLAUGHTER_SIDE).winner()).isEqualTo(Camp.GOOD);
    }

    @Test
    void allVillagersDeadWolvesWin() {
        List<PlayerEntity> players = players();
        players.stream().filter(p -> p.getRole() == Role.VILLAGER).forEach(p -> p.setAlive(false));

        assertThat(service.checkVictory(players, VictoryRule.SLAUGHTER_SIDE).winner()).isEqualTo(Camp.WEREWOLF);
    }

    @Test
    void allGodsDeadWolvesWin() {
        List<PlayerEntity> players = players();
        players.stream().filter(p -> p.getRole().category().name().equals("GOD")).forEach(p -> p.setAlive(false));

        assertThat(service.checkVictory(players, VictoryRule.SLAUGHTER_SIDE).winner()).isEqualTo(Camp.WEREWOLF);
    }

    @Test
    void slaughterAllWolvesWinWhenNotOutnumbered() {
        List<PlayerEntity> players = new ArrayList<>();
        players.add(TestFixtures.player("w1", "r", 1, Role.WEREWOLF));
        players.add(TestFixtures.player("w2", "r", 2, Role.WEREWOLF));
        players.add(TestFixtures.player("v1", "r", 3, Role.VILLAGER));
        players.add(TestFixtures.player("s1", "r", 4, Role.SEER));

        assertThat(service.checkVictory(players, VictoryRule.SLAUGHTER_ALL).winner()).isEqualTo(Camp.WEREWOLF);
    }

    private List<PlayerEntity> players() {
        return List.of(
                TestFixtures.player("w1", "r", 1, Role.WEREWOLF),
                TestFixtures.player("w2", "r", 2, Role.WEREWOLF),
                TestFixtures.player("v1", "r", 3, Role.VILLAGER),
                TestFixtures.player("v2", "r", 4, Role.VILLAGER),
                TestFixtures.player("s1", "r", 5, Role.SEER),
                TestFixtures.player("g1", "r", 6, Role.GUARD)
        );
    }
}
