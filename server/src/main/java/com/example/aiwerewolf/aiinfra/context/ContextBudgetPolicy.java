package com.example.aiwerewolf.aiinfra.context;

import com.example.aiwerewolf.memory.entity.MemoryEntryEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContextBudgetPolicy {
    private static final int MAX_VISIBLE_MEMORIES = 200;

    public List<MemoryEntryEntity> fitVisibleMemories(List<MemoryEntryEntity> memories) {
        if (memories.size() <= MAX_VISIBLE_MEMORIES) {
            return memories;
        }
        return memories.stream()
                .skip(memories.size() - MAX_VISIBLE_MEMORIES)
                .toList();
    }
}
