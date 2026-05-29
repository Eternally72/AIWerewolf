package com.example.aiwerewolf.room.dto;

public record RoleConfig(
        int werewolfCount,
        int wolfKingCount,
        int whiteWolfKingCount,
        int hiddenWolfCount,
        int villagerCount,
        int seerCount,
        int witchCount,
        int hunterCount,
        int guardCount,
        int idiotCount,
        int knightCount,
        int graveKeeperCount,
        int magicianCount,
        int cupidCount,
        int elderCount
) {
    public int total() {
        return werewolfCount + wolfKingCount + whiteWolfKingCount + hiddenWolfCount + villagerCount
                + seerCount + witchCount + hunterCount + guardCount + idiotCount + knightCount
                + graveKeeperCount + magicianCount + cupidCount + elderCount;
    }

    public int werewolfCampTotal() {
        return werewolfCount + wolfKingCount + whiteWolfKingCount + hiddenWolfCount;
    }

    public int goodCampTotal() {
        return total() - werewolfCampTotal();
    }

    public static RoleConfig sevenPlayers() {
        return new RoleConfig(2, 0, 0, 0, 3, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public static RoleConfig ninePlayers() {
        return new RoleConfig(3, 0, 0, 0, 3, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0);
    }

    public static RoleConfig twelveAdvanced() {
        return new RoleConfig(3, 1, 0, 0, 4, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0);
    }
}
