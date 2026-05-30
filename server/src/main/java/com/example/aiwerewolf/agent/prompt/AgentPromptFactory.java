package com.example.aiwerewolf.agent.prompt;

import com.example.aiwerewolf.role.model.Role;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class AgentPromptFactory {
    public String buildSystemPrompt(@Nullable Role role) {
        if (role == null) {
            return commonRule("未知身份", "只依据可见信息行动，不访问任何隐藏真相。");
        }
        return switch (role) {
            case WEREWOLF -> commonRule("狼人", "白天伪装好人，夜晚优先击杀疑似神职或强逻辑玩家，避免明显抱团。");
            case WOLF_KING -> commonRule("狼王", "可适度冲锋，死亡技能优先带走高价值好人。");
            case WHITE_WOLF_KING -> commonRule("白狼王", "评估局势是否自爆，劣势时优先带走预言家、女巫、守卫或强逻辑玩家。");
            case HIDDEN_WOLF -> commonRule("隐狼", "利用查验显示为好人的优势隐藏，不主动暴露狼队信息。");
            case VILLAGER -> commonRule("平民", "分析发言矛盾和票型，保护可信神职，不乱跳身份。");
            case SEER -> commonRule("预言家", "优先查验可疑玩家，查到狼人时倾向公开，并说明查验逻辑。");
            case WITCH -> commonRule("女巫", "谨慎使用解药和毒药，结合刀口判断狼队意图，不轻易暴露身份。");
            case HUNTER -> commonRule("猎人", "保持威慑，死亡开枪时优先选择最可疑玩家。");
            case GUARD -> commonRule("守卫", "保护疑似神职或关键玩家，记录守护路径，遵守连续守护规则。");
            case IDIOT -> commonRule("白痴", "被投票放逐时翻牌免死，翻牌后继续用发言帮助好人阵营。");
            case KNIGHT -> commonRule("骑士", "白天决斗前积累足够怀疑依据，避免误杀好人。");
            case GRAVE_KEEPER -> commonRule("守墓人", "结合放逐玩家阵营信息修正白天推理。");
            case MAGICIAN -> commonRule("魔术师", "交换技能效果以保护关键玩家或干扰狼刀，避免重复相同交换组合。");
            case CUPID -> commonRule("丘比特", "首夜选择情侣，后续根据情侣规则隐藏或引导局势。");
            case ELDER -> commonRule("长老", "利用第一次狼刀免死的韧性吸收风险，谨慎暴露身份。");
        };
    }

    private String commonRule(String roleName, String strategy) {
        return """
                你正在参加 AI 狼人杀。你的身份是：%s。
                你只能使用系统提供的私有 GameView 和可见记忆，不能猜测或访问上帝视角、数据库真相、其他 Agent 私有记忆。
                输出必须符合当前任务要求的 JSON 结构；公开发言不能泄露 strategySummary。
                角色策略：%s
                """.formatted(roleName, strategy);
    }
}
