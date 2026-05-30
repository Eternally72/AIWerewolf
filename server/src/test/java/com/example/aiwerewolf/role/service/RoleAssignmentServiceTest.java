package com.example.aiwerewolf.role.service;

import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.entity.PlayerType;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.room.dto.CreateRoomRequest;
import com.example.aiwerewolf.room.dto.HumanRoleAssignMode;
import com.example.aiwerewolf.room.dto.RoleConfig;
import com.example.aiwerewolf.room.dto.RuleConfig;
import com.example.aiwerewolf.room.dto.UiConfig;
import com.example.aiwerewolf.room.entity.HumanMode;
import com.example.aiwerewolf.room.entity.ObserverViewMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class RoleAssignmentServiceTest {
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final RoleAssignmentService service = new RoleAssignmentService(playerRepository);

    @Test
    void roleTotalMustEqualSeats() {
        assertThatThrownBy(() -> service.validateRoleConfig(request(RoleConfig.sevenPlayers(), 8, HumanMode.NONE, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("角色数量总和");
    }

    @Test
    void mustContainWerewolfAndGoodCamp() {
        RoleConfig noWolf = new RoleConfig(0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertThatThrownBy(() -> service.validateRoleConfig(request(noWolf, 6, HumanMode.NONE, null)))
                .isInstanceOf(BusinessException.class);

        RoleConfig noGood = new RoleConfig(6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertThatThrownBy(() -> service.validateRoleConfig(request(noGood, 6, HumanMode.NONE, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void specifiedHumanRoleMustExist() {
        CreateRoomRequest request = new CreateRoomRequest("r", 7, HumanMode.SINGLE_HUMAN, "me",
                HumanRoleAssignMode.SPECIFIED, Role.HUNTER, ObserverViewMode.PUBLIC_VIEW,
                RoleConfig.sevenPlayers(), RuleConfig.defaults(), UiConfig.defaults());
        assertThatThrownBy(() -> service.validateRoleConfig(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void allAiModeCreatesOnlyAiPlayersAndWolfVariantsCountAsWolves() {
        when(playerRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        RoleConfig config = new RoleConfig(1, 1, 1, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        List<PlayerEntity> players = service.assignRoles("room", request(config, 6, HumanMode.NONE, null));

        assertThat(players).hasSize(6);
        assertThat(players).allMatch(p -> p.getType() == PlayerType.AI);
        assertThat(players.stream().filter(p -> p.getRole().isWerewolfCamp()).count()).isEqualTo(4);
    }

    @Test
    void humanSpecifiedRoleIsAssigned() {
        when(playerRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        CreateRoomRequest request = new CreateRoomRequest("r", 7, HumanMode.SINGLE_HUMAN, "me",
                HumanRoleAssignMode.SPECIFIED, Role.SEER, ObserverViewMode.PUBLIC_VIEW,
                RoleConfig.sevenPlayers(), RuleConfig.defaults(), UiConfig.defaults());

        List<PlayerEntity> players = service.assignRoles("room", request);

        assertThat(players.getFirst().getType()).isEqualTo(PlayerType.HUMAN);
        assertThat(players.getFirst().getRole()).isEqualTo(Role.SEER);
    }

    private CreateRoomRequest request(RoleConfig config, int seats, HumanMode mode, Role specified) {
        return new CreateRoomRequest("r", seats, mode, mode == HumanMode.NONE ? "" : "me",
                specified == null ? HumanRoleAssignMode.RANDOM : HumanRoleAssignMode.SPECIFIED,
                specified, ObserverViewMode.GOD_VIEW, config, RuleConfig.defaults(), UiConfig.defaults());
    }
}
