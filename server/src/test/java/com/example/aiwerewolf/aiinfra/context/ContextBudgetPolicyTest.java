package com.example.aiwerewolf.aiinfra.context;

import com.example.aiwerewolf.TestFixtures;
import com.example.aiwerewolf.memory.entity.MemoryEntryEntity;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ContextBudgetPolicyTest {
    @Test
    void keepsMostRecentVisibleMemoriesWhenOverBudget() {
        ContextBudgetPolicy policy = new ContextBudgetPolicy();
        List<MemoryEntryEntity> memories = IntStream.rangeClosed(1, 205)
                .mapToObj(index -> TestFixtures.memory("m" + index, "room", MemoryScope.PUBLIC, null, "memory-" + index))
                .toList();

        List<MemoryEntryEntity> fitted = policy.fitVisibleMemories(memories);

        assertThat(fitted).hasSize(200);
        assertThat(fitted.getFirst().getContent()).isEqualTo("memory-6");
        assertThat(fitted.getLast().getContent()).isEqualTo("memory-205");
    }
}
