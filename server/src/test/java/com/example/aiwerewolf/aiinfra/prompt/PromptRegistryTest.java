package com.example.aiwerewolf.aiinfra.prompt;

import com.example.aiwerewolf.aiinfra.run.AgentRunPurpose;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.game.view.PlayerView;
import com.example.aiwerewolf.game.view.SpeechView;
import com.example.aiwerewolf.role.model.Camp;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.room.entity.RoomStatus;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PromptRegistryTest {
    private final PromptRegistry registry = new PromptRegistry(new ObjectMapper());

    @Test
    void loadsDedicatedRolePromptForEveryRole() {
        for (Role role : Role.values()) {
            String prompt = registry.systemPrompt(role);

            assertThat(prompt)
                    .contains("信息隔离")
                    .contains(role.displayName())
                    .contains("JSON");
            assertThat(registry.rolePromptVersion(role)).isEqualTo("role-prompts-v1:" + role.name());
        }
    }

    @Test
    void loadsTaskPromptsAndOutputSchemasForEveryPurpose() {
        for (AgentRunPurpose purpose : AgentRunPurpose.values()) {
            assertThat(registry.taskPrompt(purpose)).contains("JSON");
            assertThat(registry.outputSchema(purpose)).contains("\"type\": \"object\"");
            assertThat(registry.taskPromptVersion(purpose)).isEqualTo("task-prompts-v1:" + purpose.name());
            assertThat(registry.outputSchemaVersion(purpose)).isEqualTo("output-schema-v1:" + purpose.name());
        }
    }

    @Test
    void rendersAgentPromptWithPrivateViewAndSchema() {
        GameView view = new GameView("room-1", "room", RoomStatus.RUNNING, GamePhase.DAY_VOTE, 2,
                "p1", Role.SEER, Camp.GOOD,
                List.of(), List.of(), List.of(), List.of(), false);

        PromptBundle prompt = registry.buildAgentPrompt("p1", "昨晚查验 p2 为好人", view, AgentRunPurpose.VOTE);

        assertThat(prompt.systemPrompt()).contains("预言家");
        assertThat(prompt.userPrompt())
                .contains("agentRef=unknown")
                .contains("昨晚查验 p2 为好人")
                .contains("当前过滤后的私有视角")
                .contains("targetPlayerRef")
                .contains("输出 JSON Schema");
        assertThat(prompt.rolePromptVersion()).isEqualTo("role-prompts-v1:SEER");
        assertThat(prompt.taskPromptVersion()).isEqualTo("task-prompts-v1:VOTE");
        assertThat(prompt.outputSchemaVersion()).isEqualTo("output-schema-v1:VOTE");
    }

    @Test
    void promptUsesSeatReferencesAndContainsPreviousSpeeches() {
        String firstId = "uuid-player-one";
        String secondId = "uuid-player-two";
        GameView view = new GameView("room-1", "room", RoomStatus.RUNNING, GamePhase.DAY_SPEECH, 2,
                secondId, Role.SEER, Camp.GOOD,
                List.of(
                        new PlayerView(firstId, 1, "AI-1", null, true, true, true, null, null),
                        new PlayerView(secondId, 2, "AI-2", null, true, true, true, Role.SEER, Camp.GOOD)),
                List.of(),
                List.of(new SpeechView(firstId, 2, "我先分析上一夜死亡信息", null, Instant.EPOCH)),
                List.of(),
                false);

        PromptBundle prompt = registry.buildAgentPrompt(secondId, "记住 " + firstId + " 的发言", view, AgentRunPurpose.SPEECH);

        assertThat(prompt.userPrompt())
                .contains("agentRef=seat-2")
                .contains("\"playerRef\":\"seat-1\"")
                .contains("\"speech\":\"我先分析上一夜死亡信息\"")
                .contains("1号 AI-1")
                .doesNotContain(firstId)
                .doesNotContain(secondId);
    }
}
