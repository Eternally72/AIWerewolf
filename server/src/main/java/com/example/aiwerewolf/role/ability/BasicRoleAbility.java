package com.example.aiwerewolf.role.ability;

import com.example.aiwerewolf.action.entity.ActionType;
import com.example.aiwerewolf.action.entity.GameActionEntity;
import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.role.model.Camp;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.role.model.RoleCategory;

public class BasicRoleAbility implements RoleAbility {
    private final Role role;
    private final ActionType nightActionType;

    public BasicRoleAbility(Role role, ActionType nightActionType) {
        this.role = role;
        this.nightActionType = nightActionType;
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public Camp getCamp() {
        return role.camp();
    }

    @Override
    public RoleCategory getRoleCategory() {
        return role.category();
    }

    @Override
    public ActionType getNightActionType() {
        return nightActionType;
    }

    @Override
    public boolean canAct(GameView view, PlayerEntity actor) {
        return actor.isAlive() && nightActionType != ActionType.NONE;
    }

    @Override
    public String buildPrivatePrompt(GameView view) {
        return "你是" + role.displayName() + "，只能依据当前私有视角进行发言和行动。";
    }

    @Override
    public boolean validateAction(GameView view, GameActionEntity action) {
        if (role == Role.WITCH) {
            return action.getActionType() == ActionType.SAVE
                    || action.getActionType() == ActionType.POISON
                    || action.getActionType() == ActionType.NONE;
        }
        return action.getActionType() == nightActionType || action.getActionType() == ActionType.NONE;
    }

    @Override
    public String resolveAction(GameView view, GameActionEntity action) {
        return "{\"resolved\":true}";
    }
}
