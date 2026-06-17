package com.example.aiwerewolf.aiinfra.context;

import com.example.aiwerewolf.memory.entity.MemoryEntryEntity;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.role.model.Camp;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MemoryAccessPolicy {
    public boolean canViewMemory(MemoryEntryEntity entry, @Nullable String viewerPlayerId, boolean godView) {
        if (godView) {
            return true;
        }
        MemoryScope scope = entry.getScope();
        if (scope == MemoryScope.PUBLIC) {
            return true;
        }
        if (viewerPlayerId == null || viewerPlayerId.isBlank() || scope == MemoryScope.GOD_VIEW) {
            return false;
        }
        if (scope == MemoryScope.PRIVATE) {
            return viewerPlayerId.equals(entry.getOwnerPlayerId());
        }
        return visiblePlayerIds(entry).contains(viewerPlayerId);
    }

    public boolean canViewPlayerIdentity(PlayerEntity viewer, PlayerEntity target, Set<String> loverIds) {
        if (target.getId().equals(viewer.getId())) {
            return true;
        }
        if (isWerewolfMate(viewer, target)) {
            return true;
        }
        if (loverIds.contains(target.getId())) {
            return true;
        }
        return viewer.getCamp() == Camp.THIRD_PARTY && target.getCamp() == Camp.THIRD_PARTY;
    }

    private boolean isWerewolfMate(PlayerEntity viewer, PlayerEntity target) {
        return viewer.getRole() != null
                && target.getRole() != null
                && viewer.getRole().isWerewolfCamp()
                && target.getRole().isWerewolfCamp();
    }

    private Set<String> visiblePlayerIds(MemoryEntryEntity entry) {
        String ids = entry.getVisibleToPlayerIds();
        if (ids == null || ids.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(ids.split(","))
                .map(String::strip)
                .filter(id -> !id.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
