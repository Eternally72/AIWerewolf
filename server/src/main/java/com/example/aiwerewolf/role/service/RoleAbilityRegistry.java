package com.example.aiwerewolf.role.service;

import com.example.aiwerewolf.action.entity.ActionType;
import com.example.aiwerewolf.role.ability.BasicRoleAbility;
import com.example.aiwerewolf.role.ability.RoleAbility;
import com.example.aiwerewolf.role.model.Role;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class RoleAbilityRegistry {
    private final Map<Role, RoleAbility> abilities = new EnumMap<>(Role.class);

    public RoleAbilityRegistry() {
        register(new BasicRoleAbility(Role.WEREWOLF, ActionType.KILL));
        register(new BasicRoleAbility(Role.WOLF_KING, ActionType.KILL));
        register(new BasicRoleAbility(Role.WHITE_WOLF_KING, ActionType.KILL));
        register(new BasicRoleAbility(Role.HIDDEN_WOLF, ActionType.NONE));
        register(new BasicRoleAbility(Role.VILLAGER, ActionType.NONE));
        register(new BasicRoleAbility(Role.SEER, ActionType.CHECK));
        register(new BasicRoleAbility(Role.WITCH, ActionType.SAVE));
        register(new BasicRoleAbility(Role.HUNTER, ActionType.NONE));
        register(new BasicRoleAbility(Role.GUARD, ActionType.GUARD));
        register(new BasicRoleAbility(Role.IDIOT, ActionType.NONE));
        register(new BasicRoleAbility(Role.KNIGHT, ActionType.NONE));
        register(new BasicRoleAbility(Role.GRAVE_KEEPER, ActionType.NONE));
        register(new BasicRoleAbility(Role.MAGICIAN, ActionType.SWAP));
        register(new BasicRoleAbility(Role.CUPID, ActionType.LINK_LOVERS));
        register(new BasicRoleAbility(Role.ELDER, ActionType.NONE));
    }

    public RoleAbility get(Role role) {
        return abilities.get(role);
    }

    public List<RoleAbility> list() {
        return List.copyOf(abilities.values());
    }

    private void register(RoleAbility ability) {
        abilities.put(ability.getRole(), ability);
    }
}
