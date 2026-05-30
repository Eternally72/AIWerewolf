package com.example.aiwerewolf.vote.service;

import jakarta.validation.constraints.NotBlank;
import org.springframework.lang.Nullable;

public record VoteRequest(@NotBlank String targetPlayerId, @Nullable String reason) {
}
