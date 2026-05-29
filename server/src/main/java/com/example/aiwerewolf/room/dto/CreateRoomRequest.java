package com.example.aiwerewolf.room.dto;

import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.room.entity.HumanMode;
import com.example.aiwerewolf.room.entity.ObserverViewMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRoomRequest(
        @NotBlank String roomName,
        @Min(6) @Max(18) int totalSeats,
        @NotNull HumanMode humanMode,
        String humanPlayerName,
        @NotNull HumanRoleAssignMode humanRoleAssignMode,
        Role specifiedHumanRole,
        @NotNull ObserverViewMode observerViewMode,
        @NotNull @Valid RoleConfig roleConfig,
        @NotNull @Valid RuleConfig ruleConfig,
        @NotNull @Valid UiConfig uiConfig
) {
    public static CreateRoomRequest defaultSevenAi() {
        return new CreateRoomRequest(
                "AI 7人标准局",
                7,
                HumanMode.NONE,
                "",
                HumanRoleAssignMode.RANDOM,
                null,
                ObserverViewMode.GOD_VIEW,
                RoleConfig.sevenPlayers(),
                RuleConfig.defaults(),
                UiConfig.defaults()
        );
    }
}
