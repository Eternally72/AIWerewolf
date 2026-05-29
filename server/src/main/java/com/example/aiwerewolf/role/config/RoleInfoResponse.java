package com.example.aiwerewolf.role.config;

import com.example.aiwerewolf.role.model.Camp;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.role.model.RoleCategory;

public record RoleInfoResponse(Role role, String displayName, Camp camp, RoleCategory category, String ability) {
}
