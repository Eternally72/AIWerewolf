package com.example.aiwerewolf.agent.prompt;

import com.example.aiwerewolf.role.model.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPromptFactoryTest {
    private final AgentPromptFactory promptFactory = new AgentPromptFactory();

    @Test
    void loadsDedicatedPromptForEveryRole() {
        for (Role role : Role.values()) {
            String prompt = promptFactory.buildSystemPrompt(role);

            assertThat(prompt)
                    .contains("信息隔离")
                    .contains(role.displayName())
                    .contains("JSON");
        }
    }

    @Test
    void unknownRolePromptKeepsInformationIsolationRules() {
        String prompt = promptFactory.buildSystemPrompt(null);

        assertThat(prompt)
                .contains("身份暂未")
                .contains("不能自行补全身份")
                .contains("上帝视角");
    }
}
