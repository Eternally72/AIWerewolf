package com.example.aiwerewolf.speech.service;

import com.example.aiwerewolf.role.model.Role;
import jakarta.validation.constraints.NotBlank;

public record SpeechRequest(@NotBlank String content, Role claimedRole) {
}
