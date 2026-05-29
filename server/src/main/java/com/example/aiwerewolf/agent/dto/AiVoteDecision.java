package com.example.aiwerewolf.agent.dto;

public record AiVoteDecision(String targetPlayerId, String reason, double confidence) {
}
