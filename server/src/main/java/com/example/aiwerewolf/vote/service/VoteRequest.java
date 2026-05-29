package com.example.aiwerewolf.vote.service;

import jakarta.validation.constraints.NotBlank;

public record VoteRequest(@NotBlank String targetPlayerId, String reason) {
}
