package com.example.aiwerewolf.memory.service;

import com.example.aiwerewolf.aiinfra.context.ContextBudgetPolicy;
import com.example.aiwerewolf.aiinfra.context.MemoryAccessPolicy;
import com.example.aiwerewolf.game.event.GameEventService;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.memory.entity.MemoryEntryEntity;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import com.example.aiwerewolf.memory.repository.MemoryEntryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemoryService {
    private final MemoryEntryRepository memoryEntryRepository;
    private final AgentShortTermMemoryService shortTermMemoryService;
    private final MemoryAccessPolicy memoryAccessPolicy;
    private final ContextBudgetPolicy contextBudgetPolicy;
    private final GameEventService gameEventService;

    public MemoryService(MemoryEntryRepository memoryEntryRepository,
                         AgentShortTermMemoryService shortTermMemoryService,
                         MemoryAccessPolicy memoryAccessPolicy,
                         ContextBudgetPolicy contextBudgetPolicy,
                         GameEventService gameEventService) {
        this.memoryEntryRepository = memoryEntryRepository;
        this.shortTermMemoryService = shortTermMemoryService;
        this.memoryAccessPolicy = memoryAccessPolicy;
        this.contextBudgetPolicy = contextBudgetPolicy;
        this.gameEventService = gameEventService;
    }

    public MemoryEntryEntity appendPublicMemory(String roomId, int round, GamePhase phase, String type, String content) {
        return append(roomId, round, phase, MemoryScope.PUBLIC, null, null, type, content, "{}");
    }

    public MemoryEntryEntity appendPrivateMemory(String roomId, int round, GamePhase phase, String playerId, String type, String content) {
        MemoryEntryEntity entry = append(roomId, round, phase, MemoryScope.PRIVATE, playerId, playerId, type, content, "{}");
        shortTermMemoryService.appendObservation(roomId, playerId, content);
        return entry;
    }

    public MemoryEntryEntity appendSharedSecretMemory(String roomId, int round, GamePhase phase, MemoryScope scope,
                                                      List<String> visibleTo, String type, String content) {
        MemoryEntryEntity entry = append(roomId, round, phase, scope, null, String.join(",", visibleTo), type, content, "{}");
        visibleTo.forEach(playerId -> shortTermMemoryService.appendObservation(roomId, playerId, content));
        return entry;
    }

    public MemoryEntryEntity appendGodViewMemory(String roomId, int round, GamePhase phase, String type, String content) {
        return append(roomId, round, phase, MemoryScope.GOD_VIEW, null, null, type, content, "{}");
    }

    public List<MemoryEntryEntity> listVisibleMemoriesForPlayer(String roomId, String playerId) {
        List<MemoryEntryEntity> visible = memoryEntryRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .filter(entry -> memoryAccessPolicy.canViewMemory(entry, playerId, false))
                .toList();
        return contextBudgetPolicy.fitVisibleMemories(visible);
    }

    public List<MemoryEntryEntity> listGodViewMemories(String roomId) {
        return memoryEntryRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
    }

    private MemoryEntryEntity append(String roomId, int round, GamePhase phase, MemoryScope scope, String owner,
                                     String visibleTo, String type, String content, String metadataJson) {
        MemoryEntryEntity entry = new MemoryEntryEntity();
        entry.setRoomId(roomId);
        entry.setRoundNumber(round);
        entry.setPhase(phase);
        entry.setScope(scope);
        entry.setOwnerPlayerId(owner);
        entry.setVisibleToPlayerIds(visibleTo);
        entry.setEventType(type);
        entry.setContent(content);
        entry.setMetadataJson(metadataJson);
        MemoryEntryEntity saved = memoryEntryRepository.save(entry);
        gameEventService.appendMemoryEvent(roomId, round, phase, scope, owner, visibleTo, type, content, metadataJson);
        return saved;
    }

}
