package com.example.aiwerewolf.room.dto;

import java.util.List;

public record DefaultConfigResponse(List<TemplateConfig> templates) {
    public record TemplateConfig(String key, String name, int seats, RoleConfig roleConfig, RuleConfig ruleConfig, UiConfig uiConfig) {
    }
}
