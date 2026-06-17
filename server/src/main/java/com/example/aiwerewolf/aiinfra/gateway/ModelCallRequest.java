package com.example.aiwerewolf.aiinfra.gateway;

import com.example.aiwerewolf.aiinfra.run.AgentRunPurpose;

public record ModelCallRequest(
        String roomId,
        String agentId,
        AgentRunPurpose purpose,
        String systemPrompt,
        String userPrompt
) {
}
