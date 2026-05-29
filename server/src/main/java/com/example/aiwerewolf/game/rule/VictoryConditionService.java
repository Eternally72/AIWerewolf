package com.example.aiwerewolf.game.rule;

import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.role.model.Camp;
import com.example.aiwerewolf.role.model.RoleCategory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VictoryConditionService {
    public VictoryResult checkVictory(List<PlayerEntity> players, VictoryRule rule) {
        long aliveWolves = players.stream().filter(PlayerEntity::isAlive).filter(p -> p.getRole().isWerewolfCamp()).count();
        long aliveGood = players.stream().filter(PlayerEntity::isAlive).filter(p -> p.getCamp() == Camp.GOOD).count();
        long aliveVillagers = players.stream().filter(PlayerEntity::isAlive).filter(p -> p.getRoleCategory() == RoleCategory.VILLAGER).count();
        long aliveGods = players.stream().filter(PlayerEntity::isAlive).filter(p -> p.getRoleCategory() == RoleCategory.GOD).count();

        if (aliveWolves == 0) {
            return VictoryResult.win(Camp.GOOD, "所有狼人阵营角色已死亡");
        }
        if (rule == VictoryRule.SLAUGHTER_ALL) {
            if (aliveGood == 0 || aliveWolves >= aliveGood) {
                return VictoryResult.win(Camp.WEREWOLF, "狼人数量已大于等于好人数量");
            }
            return VictoryResult.ongoing();
        }
        if (rule == VictoryRule.LOVERS_THIRD_PARTY) {
            return checkVictory(players, VictoryRule.SLAUGHTER_SIDE);
        }
        if (aliveVillagers == 0) {
            return VictoryResult.win(Camp.WEREWOLF, "所有平民类角色已死亡");
        }
        if (aliveGods == 0) {
            return VictoryResult.win(Camp.WEREWOLF, "所有神职角色已死亡");
        }
        return VictoryResult.ongoing();
    }
}
