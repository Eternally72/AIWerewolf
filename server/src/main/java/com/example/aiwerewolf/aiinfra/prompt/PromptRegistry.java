package com.example.aiwerewolf.aiinfra.prompt;

import com.example.aiwerewolf.aiinfra.run.AgentRunPurpose;
import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.role.model.Role;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

@Component
public class PromptRegistry {
    private static final String ROLE_PROMPT_VERSION = "role-prompts-v1";
    private static final String TASK_PROMPT_VERSION = "task-prompts-v1";
    private static final String OUTPUT_SCHEMA_VERSION = "output-schema-v1";
    private static final String ROLE_ROOT = "prompts/roles/";
    private static final String TASK_ROOT = "prompts/tasks/";
    private static final String SCHEMA_ROOT = "prompts/output-schema/";

    private final String commonRolePrompt;
    private final String unknownRolePrompt;
    private final Map<Role, String> rolePrompts = new EnumMap<>(Role.class);
    private final Map<AgentRunPurpose, String> taskPrompts = new EnumMap<>(AgentRunPurpose.class);
    private final Map<AgentRunPurpose, String> outputSchemas = new EnumMap<>(AgentRunPurpose.class);
    private final ObjectMapper objectMapper;

    public PromptRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.commonRolePrompt = load(ROLE_ROOT + "common.md");
        this.unknownRolePrompt = commonRolePrompt + "\n\n" + load(ROLE_ROOT + "unknown.md");
        for (Role role : Role.values()) {
            rolePrompts.put(role, commonRolePrompt + "\n\n" + load(ROLE_ROOT + roleFileName(role)));
        }
        taskPrompts.put(AgentRunPurpose.SPEECH, load(TASK_ROOT + "speech.md"));
        taskPrompts.put(AgentRunPurpose.VOTE, load(TASK_ROOT + "vote.md"));
        taskPrompts.put(AgentRunPurpose.NIGHT_ACTION, load(TASK_ROOT + "night-action.md"));
        taskPrompts.put(AgentRunPurpose.DAY_SKILL, load(TASK_ROOT + "day-skill.md"));

        outputSchemas.put(AgentRunPurpose.SPEECH, load(SCHEMA_ROOT + "speech.schema.json"));
        outputSchemas.put(AgentRunPurpose.VOTE, load(SCHEMA_ROOT + "vote.schema.json"));
        outputSchemas.put(AgentRunPurpose.NIGHT_ACTION, load(SCHEMA_ROOT + "night-action.schema.json"));
        outputSchemas.put(AgentRunPurpose.DAY_SKILL, load(SCHEMA_ROOT + "day-skill.schema.json"));
    }

    public PromptBundle buildAgentPrompt(String agentId,
                                         String shortTermMemory,
                                         GameView privateGameView,
                                         AgentRunPurpose purpose) {
        String taskPrompt = taskPrompt(purpose);
        String outputSchema = outputSchema(purpose);
        AgentPromptContext promptContext = AgentPromptContext.from(privateGameView);
        String userPrompt = """
                agentRef=%s

                短期记忆：
                %s

                当前过滤后的私有视角 JSON：
                %s

                任务说明：
                %s

                输出 JSON Schema：
                %s
                """.formatted(promptContext.viewer(), sanitizeMemory(shortTermMemory, privateGameView),
                toJson(promptContext), taskPrompt, outputSchema);
        return new PromptBundle(
                systemPrompt(privateGameView.ownRole()),
                userPrompt,
                promptContext,
                rolePromptVersion(privateGameView.ownRole()),
                taskPromptVersion(purpose),
                outputSchemaVersion(purpose));
    }

    public String systemPrompt(@Nullable Role role) {
        if (role == null) {
            return unknownRolePrompt;
        }
        String prompt = rolePrompts.get(role);
        if (prompt == null) {
            throw new IllegalStateException("Missing prompt for role: " + role);
        }
        return prompt;
    }

    public String taskPrompt(AgentRunPurpose purpose) {
        String prompt = taskPrompts.get(purpose);
        if (prompt == null) {
            throw new IllegalStateException("Missing task prompt for purpose: " + purpose);
        }
        return prompt;
    }

    public String outputSchema(AgentRunPurpose purpose) {
        String schema = outputSchemas.get(purpose);
        if (schema == null) {
            throw new IllegalStateException("Missing output schema for purpose: " + purpose);
        }
        return schema;
    }

    public String rolePromptVersion(@Nullable Role role) {
        return role == null ? ROLE_PROMPT_VERSION + ":unknown" : ROLE_PROMPT_VERSION + ":" + role.name();
    }

    public String taskPromptVersion(AgentRunPurpose purpose) {
        return TASK_PROMPT_VERSION + ":" + purpose.name();
    }

    public String outputSchemaVersion(AgentRunPurpose purpose) {
        return OUTPUT_SCHEMA_VERSION + ":" + purpose.name();
    }

    private String roleFileName(Role role) {
        return role.name().toLowerCase(Locale.ROOT) + ".md";
    }

    private String load(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new IllegalStateException("Prompt resource not found: " + path);
        }
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load prompt resource: " + path, ex);
        }
    }

    private String blankToPlaceholder(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return "暂无";
        }
        return value;
    }

    private String sanitizeMemory(@Nullable String memory, GameView view) {
        String sanitized = blankToPlaceholder(memory);
        for (var player : view.players()) {
            sanitized = sanitized.replace(player.id(), player.seatNumber() + "号 " + player.name());
        }
        return sanitized;
    }

    private String toJson(AgentPromptContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize agent prompt context", ex);
        }
    }
}
