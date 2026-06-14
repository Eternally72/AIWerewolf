package com.example.aiwerewolf.memory.service;

import com.example.aiwerewolf.TestFixtures;
import com.example.aiwerewolf.memory.entity.MemoryEntryEntity;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import com.example.aiwerewolf.memory.repository.MemoryEntryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemoryServiceTest {
    @Test
    void sharedSecretMemoryIsVisibleOnlyToListedPlayers() {
        String roomId = "room";
        MemoryEntryRepository repository = mock(MemoryEntryRepository.class);
        AgentShortTermMemoryService shortTermMemoryService = mock(AgentShortTermMemoryService.class);
        MemoryService service = new MemoryService(repository, shortTermMemoryService);

        MemoryEntryEntity publicMemory = TestFixtures.memory("m1", roomId, MemoryScope.PUBLIC, null, "public");
        MemoryEntryEntity wolfMemory = TestFixtures.memory("m2", roomId, MemoryScope.WEREWOLF_TEAM, null, "wolf secret");
        wolfMemory.setVisibleToPlayerIds("wolf1,wolf2");
        when(repository.findByRoomIdOrderByCreatedAtAsc(roomId)).thenReturn(List.of(publicMemory, wolfMemory));

        assertThat(service.listVisibleMemoriesForPlayer(roomId, "wolf1")).extracting(MemoryEntryEntity::getContent)
                .containsExactly("public", "wolf secret");
        assertThat(service.listVisibleMemoriesForPlayer(roomId, "villager")).extracting(MemoryEntryEntity::getContent)
                .containsExactly("public");
    }
}
