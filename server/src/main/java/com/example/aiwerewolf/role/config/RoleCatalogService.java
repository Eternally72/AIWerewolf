package com.example.aiwerewolf.role.config;

import com.example.aiwerewolf.role.model.Role;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class RoleCatalogService {
    public List<RoleInfoResponse> listRoles() {
        return Arrays.stream(Role.values())
                .map(role -> new RoleInfoResponse(role, role.displayName(), role.camp(), role.category(), ability(role)))
                .toList();
    }

    private String ability(Role role) {
        return switch (role) {
            case WEREWOLF -> "夜晚参与狼队击杀，知道狼人队友。";
            case WOLF_KING -> "死亡时可开枪带走一名玩家。";
            case WHITE_WOLF_KING -> "白天可自爆并带走一名玩家。";
            case HIDDEN_WOLF -> "查验显示为好人，第一版知道狼队但不参与狼刀。";
            case VILLAGER -> "无夜间技能，依靠发言和投票找狼。";
            case SEER -> "每晚查验一名玩家阵营。";
            case WITCH -> "拥有一瓶解药和一瓶毒药。";
            case HUNTER -> "死亡时可开枪。";
            case GUARD -> "每晚守护一名玩家免疫狼刀。";
            case IDIOT -> "被投票放逐时可翻牌免死。";
            case KNIGHT -> "白天可决斗一名玩家。";
            case GRAVE_KEEPER -> "夜晚得知白天放逐玩家阵营。";
            case MAGICIAN -> "交换两名玩家受到的技能效果。";
            case CUPID -> "首夜连接两名情侣。";
            case ELDER -> "第一次被狼刀时免死。";
        };
    }
}
