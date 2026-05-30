package com.example.aiwerewolf.agent.prompt;

import com.example.aiwerewolf.role.model.Role;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class AgentPromptFactory {
    private static final String PROMPT_ROOT = "prompts/roles/";

    private final String commonPrompt;
    private final String unknownPrompt;
    private final Map<Role, String> rolePrompts = new EnumMap<>(Role.class);

    public AgentPromptFactory() {
        this.commonPrompt = loadPrompt("common.md");
        this.unknownPrompt = commonPrompt + "\n\n" + loadPrompt("unknown.md");
        for (Role role : Role.values()) {
            rolePrompts.put(role, commonPrompt + "\n\n" + loadPrompt(fileName(role)));
        }
    }

    public String buildSystemPrompt(@Nullable Role role) {
        if (role == null) {
            return unknownPrompt;
        }
        String prompt = rolePrompts.get(role);
        if (prompt == null) {
            throw new IllegalStateException("Missing prompt for role: " + role);
        }
        return prompt;
    }

    private String fileName(Role role) {
        return role.name().toLowerCase(Locale.ROOT) + ".md";
    }

    private String loadPrompt(String fileName) {
        ClassPathResource resource = new ClassPathResource(PROMPT_ROOT + fileName);
        if (!resource.exists()) {
            throw new IllegalStateException("Prompt resource not found: " + PROMPT_ROOT + fileName);
        }
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load prompt resource: " + PROMPT_ROOT + fileName, ex);
        }
    }
}
