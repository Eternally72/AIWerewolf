package com.example.aiwerewolf.speech.service;

import com.example.aiwerewolf.role.model.Role;
import jakarta.validation.constraints.NotBlank;
import org.springframework.lang.Nullable;

public record SpeechRequest(@NotBlank String content, @Nullable Role claimedRole) {
}
