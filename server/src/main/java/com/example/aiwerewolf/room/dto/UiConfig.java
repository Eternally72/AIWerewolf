package com.example.aiwerewolf.room.dto;

public record UiConfig(
        String theme,
        String animationLevel,
        boolean enableSoundEffect,
        boolean showRoleAvatar,
        boolean showTimeline,
        boolean showGodViewPanel
) {
    public static UiConfig defaults() {
        return new UiConfig("dark-moon", "smooth", false, true, true, true);
    }
}
