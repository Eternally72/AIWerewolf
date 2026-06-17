package com.example.aiwerewolf.aiinfra.context;

import com.example.aiwerewolf.TestFixtures;
import com.example.aiwerewolf.memory.entity.MemoryEntryEntity;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.role.model.Role;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryAccessPolicyTest {
    private final MemoryAccessPolicy policy = new MemoryAccessPolicy();

    @Test
    void memoryVisibilityHonorsScopeOwnerVisibleListAndGodView() {
        MemoryEntryEntity publicMemory = TestFixtures.memory("m1", "room", MemoryScope.PUBLIC, null, "public");
        MemoryEntryEntity privateMemory = TestFixtures.memory("m2", "room", MemoryScope.PRIVATE, "seer", "check");
        MemoryEntryEntity wolfMemory = TestFixtures.memory("m3", "room", MemoryScope.WEREWOLF_TEAM, null, "wolf");
        wolfMemory.setVisibleToPlayerIds("wolf1, wolf2");
        MemoryEntryEntity godMemory = TestFixtures.memory("m4", "room", MemoryScope.GOD_VIEW, null, "god");

        assertThat(policy.canViewMemory(publicMemory, "villager", false)).isTrue();
        assertThat(policy.canViewMemory(privateMemory, "seer", false)).isTrue();
        assertThat(policy.canViewMemory(privateMemory, "villager", false)).isFalse();
        assertThat(policy.canViewMemory(wolfMemory, "wolf2", false)).isTrue();
        assertThat(policy.canViewMemory(wolfMemory, "villager", false)).isFalse();
        assertThat(policy.canViewMemory(godMemory, "seer", false)).isFalse();
        assertThat(policy.canViewMemory(godMemory, null, true)).isTrue();
    }

    @Test
    void playerIdentityVisibilityHonorsSelfWolfTeamLoversAndThirdParty() {
        PlayerEntity wolf = TestFixtures.player("wolf", "room", 1, Role.WEREWOLF);
        PlayerEntity wolfKing = TestFixtures.player("wolfKing", "room", 2, Role.WOLF_KING);
        PlayerEntity seer = TestFixtures.player("seer", "room", 3, Role.SEER);
        PlayerEntity hiddenWolf = TestFixtures.player("hidden", "room", 4, Role.HIDDEN_WOLF);

        assertThat(policy.canViewPlayerIdentity(wolf, wolf, Set.of())).isTrue();
        assertThat(policy.canViewPlayerIdentity(wolf, wolfKing, Set.of())).isTrue();
        assertThat(policy.canViewPlayerIdentity(wolf, seer, Set.of())).isFalse();
        assertThat(policy.canViewPlayerIdentity(seer, wolf, Set.of("wolf"))).isTrue();
        assertThat(policy.canViewPlayerIdentity(wolf, hiddenWolf, Set.of())).isTrue();
    }
}
