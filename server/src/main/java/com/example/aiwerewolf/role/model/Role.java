package com.example.aiwerewolf.role.model;

public enum Role {
    WEREWOLF(Camp.WEREWOLF, RoleCategory.WEREWOLF, "狼人"),
    WOLF_KING(Camp.WEREWOLF, RoleCategory.WEREWOLF, "狼王"),
    WHITE_WOLF_KING(Camp.WEREWOLF, RoleCategory.WEREWOLF, "白狼王"),
    HIDDEN_WOLF(Camp.WEREWOLF, RoleCategory.WEREWOLF, "隐狼"),
    VILLAGER(Camp.GOOD, RoleCategory.VILLAGER, "平民"),
    SEER(Camp.GOOD, RoleCategory.GOD, "预言家"),
    WITCH(Camp.GOOD, RoleCategory.GOD, "女巫"),
    HUNTER(Camp.GOOD, RoleCategory.GOD, "猎人"),
    GUARD(Camp.GOOD, RoleCategory.GOD, "守卫"),
    IDIOT(Camp.GOOD, RoleCategory.VILLAGER, "白痴"),
    KNIGHT(Camp.GOOD, RoleCategory.GOD, "骑士"),
    GRAVE_KEEPER(Camp.GOOD, RoleCategory.GOD, "守墓人"),
    MAGICIAN(Camp.GOOD, RoleCategory.GOD, "魔术师"),
    CUPID(Camp.GOOD, RoleCategory.GOD, "丘比特"),
    ELDER(Camp.GOOD, RoleCategory.VILLAGER, "长老");

    private final Camp camp;
    private final RoleCategory category;
    private final String displayName;

    Role(Camp camp, RoleCategory category, String displayName) {
        this.camp = camp;
        this.category = category;
        this.displayName = displayName;
    }

    public Camp camp() {
        return camp;
    }

    public RoleCategory category() {
        return category;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isWerewolfCamp() {
        return camp == Camp.WEREWOLF;
    }
}
