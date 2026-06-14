package com.example.aiwerewolf.role.ability;

import com.example.aiwerewolf.action.entity.GameActionEntity;
import com.example.aiwerewolf.action.entity.ActionType;
import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.role.model.Camp;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.role.model.RoleCategory;

public interface RoleAbility {
    Role getRole();
    Camp getCamp();
    RoleCategory getRoleCategory();
    ActionType getNightActionType();
    boolean canAct(GameView view, PlayerEntity actor);
    String buildPrivatePrompt(GameView view);
    boolean validateAction(GameView view, GameActionEntity action);
    String resolveAction(GameView view, GameActionEntity action);
    default void onDeath(PlayerEntity deadPlayer) {
    }
}
