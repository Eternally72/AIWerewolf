package com.example.aiwerewolf.role.service;

import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.entity.PlayerType;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.room.dto.CreateRoomRequest;
import com.example.aiwerewolf.room.dto.HumanRoleAssignMode;
import com.example.aiwerewolf.room.dto.RoleConfig;
import com.example.aiwerewolf.room.entity.HumanMode;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class RoleAssignmentService {
    private final PlayerRepository playerRepository;
    private final SecureRandom random = new SecureRandom();

    public RoleAssignmentService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public void validateRoleConfig(CreateRoomRequest request) {
        RoleConfig config = request.roleConfig();
        if (request.totalSeats() < 6 || request.totalSeats() > 18) {
            throw new BusinessException("INVALID_ROLE_CONFIG", "总座位数必须在 6 到 18 之间");
        }
        if (config.total() != request.totalSeats()) {
            throw new BusinessException("INVALID_ROLE_CONFIG", "角色数量总和必须等于总座位数");
        }
        if (config.werewolfCampTotal() < 1) {
            throw new BusinessException("INVALID_ROLE_CONFIG", "至少需要 1 名狼人阵营角色");
        }
        if (config.goodCampTotal() < 1) {
            throw new BusinessException("INVALID_ROLE_CONFIG", "至少需要 1 名好人阵营角色");
        }
        if (request.humanMode() == HumanMode.NONE && request.humanPlayerName() != null && !request.humanPlayerName().isBlank()) {
            throw new BusinessException("INVALID_ROLE_CONFIG", "全 AI 模式下不能配置真人玩家");
        }
        if (request.humanRoleAssignMode() == HumanRoleAssignMode.SPECIFIED) {
            if (request.humanMode() == HumanMode.NONE) {
                throw new BusinessException("INVALID_ROLE_CONFIG", "观众模式下用户不能被分配角色");
            }
            if (request.specifiedHumanRole() == null || countRole(config, request.specifiedHumanRole()) <= 0) {
                throw new BusinessException("INVALID_ROLE_CONFIG", "指定真人角色必须在角色配置中存在");
            }
        }
    }

    public List<PlayerEntity> assignRoles(String roomId, CreateRoomRequest request) {
        validateRoleConfig(request);
        List<Role> deck = buildRoleDeck(request.roleConfig());
        shuffleRoles(deck);

        List<PlayerEntity> players = new ArrayList<>();
        int seat = 1;
        if (request.humanMode() == HumanMode.SINGLE_HUMAN) {
            Role humanRole = assignHumanRole(deck, request);
            players.add(buildPlayer(roomId, seat++, playerName(request.humanPlayerName()), PlayerType.HUMAN, humanRole));
        }
        for (; seat <= request.totalSeats(); seat++) {
            Role role = deck.removeFirst();
            players.add(buildPlayer(roomId, seat, "AI-" + seat, PlayerType.AI, role));
        }
        return playerRepository.saveAll(players);
    }

    public Role assignHumanRole(List<Role> deck, CreateRoomRequest request) {
        if (request.humanRoleAssignMode() == HumanRoleAssignMode.SPECIFIED) {
            Role role = request.specifiedHumanRole();
            deck.remove(role);
            return role;
        }
        return deck.remove(random.nextInt(deck.size()));
    }

    public List<Role> buildRoleDeck(RoleConfig config) {
        List<Role> roles = new ArrayList<>();
        add(roles, Role.WEREWOLF, config.werewolfCount());
        add(roles, Role.WOLF_KING, config.wolfKingCount());
        add(roles, Role.WHITE_WOLF_KING, config.whiteWolfKingCount());
        add(roles, Role.HIDDEN_WOLF, config.hiddenWolfCount());
        add(roles, Role.VILLAGER, config.villagerCount());
        add(roles, Role.SEER, config.seerCount());
        add(roles, Role.WITCH, config.witchCount());
        add(roles, Role.HUNTER, config.hunterCount());
        add(roles, Role.GUARD, config.guardCount());
        add(roles, Role.IDIOT, config.idiotCount());
        add(roles, Role.KNIGHT, config.knightCount());
        add(roles, Role.GRAVE_KEEPER, config.graveKeeperCount());
        add(roles, Role.MAGICIAN, config.magicianCount());
        add(roles, Role.CUPID, config.cupidCount());
        add(roles, Role.ELDER, config.elderCount());
        return roles;
    }

    public void shuffleRoles(List<Role> roles) {
        Collections.shuffle(roles, random);
    }

    private PlayerEntity buildPlayer(String roomId, int seat, String name, PlayerType type, Role role) {
        PlayerEntity player = new PlayerEntity();
        player.setRoomId(roomId);
        player.setSeatNumber(seat);
        player.setName(name);
        player.setType(type);
        player.setRole(role);
        player.setCamp(role.camp());
        player.setRoleCategory(role.category());
        player.setHost(type == PlayerType.HUMAN && seat == 1);
        player.setObserver(false);
        return player;
    }

    private String playerName(String name) {
        return name == null || name.isBlank() ? "真人玩家" : name;
    }

    private int countRole(RoleConfig config, Role role) {
        return switch (role) {
            case WEREWOLF -> config.werewolfCount();
            case WOLF_KING -> config.wolfKingCount();
            case WHITE_WOLF_KING -> config.whiteWolfKingCount();
            case HIDDEN_WOLF -> config.hiddenWolfCount();
            case VILLAGER -> config.villagerCount();
            case SEER -> config.seerCount();
            case WITCH -> config.witchCount();
            case HUNTER -> config.hunterCount();
            case GUARD -> config.guardCount();
            case IDIOT -> config.idiotCount();
            case KNIGHT -> config.knightCount();
            case GRAVE_KEEPER -> config.graveKeeperCount();
            case MAGICIAN -> config.magicianCount();
            case CUPID -> config.cupidCount();
            case ELDER -> config.elderCount();
        };
    }

    private void add(List<Role> roles, Role role, int count) {
        for (int i = 0; i < count; i++) {
            roles.add(role);
        }
    }
}
