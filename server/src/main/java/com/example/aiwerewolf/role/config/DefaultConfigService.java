package com.example.aiwerewolf.role.config;

import com.example.aiwerewolf.room.dto.DefaultConfigResponse;
import com.example.aiwerewolf.room.dto.RoleConfig;
import com.example.aiwerewolf.room.dto.RuleConfig;
import com.example.aiwerewolf.room.dto.UiConfig;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultConfigService {
    public DefaultConfigResponse defaults() {
        RuleConfig rule = RuleConfig.defaults();
        UiConfig ui = UiConfig.defaults();
        return new DefaultConfigResponse(List.of(
                new DefaultConfigResponse.TemplateConfig("7-standard", "7 人标准局", 7, RoleConfig.sevenPlayers(), rule, ui),
                new DefaultConfigResponse.TemplateConfig("9-standard", "9 人标准局", 9, RoleConfig.ninePlayers(), rule, ui),
                new DefaultConfigResponse.TemplateConfig("12-advanced", "12 人进阶局", 12, RoleConfig.twelveAdvanced(), rule, ui),
                new DefaultConfigResponse.TemplateConfig("12-complex", "12 人复杂局", 12,
                        new RoleConfig(3, 0, 1, 0, 4, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0), rule, ui)
        ));
    }
}
