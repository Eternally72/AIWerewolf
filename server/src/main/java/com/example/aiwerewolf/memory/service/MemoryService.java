package com.example.aiwerewolf.memory.service;

import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.memory.entity.MemoryEntryEntity;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import com.example.aiwerewolf.memory.repository.MemoryEntryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemoryService {
    private final MemoryEntryRepository memoryEntryRepository;

    public MemoryService(MemoryEntryRepository memoryEntryRepository) {
        this.memoryEntryRepository = memoryEntryRepository;
    }

    public MemoryEntryEntity appendPublicMemory(String roomId, int round, GamePhase phase, String type, String content) {
        return append(roomId, round, phase, MemoryScope.PUBLIC, null, null, type, content, "{}");
    }

    public MemoryEntryEntity appendPrivateMemory(String roomId, int round, GamePhase phase, String playerId, String type, String content) {
        return append(roomId, round, phase, MemoryScope.PRIVATE, playerId, playerId, type, content, "{}");
    }

    public MemoryEntryEntity appendSharedSecretMemory(String roomId, int round, GamePhase phase, MemoryScope scope,
                                                      List<String> visibleTo, String type, String content) {
        return append(roomId, round, phase, scope, null, String.join(",", visibleTo), type, content, "{}");
    }

    public MemoryEntryEntity appendGodViewMemory(String roomId, int round, GamePhase phase, String type, String content) {
        return append(roomId, round, phase, MemoryScope.GOD_VIEW, null, null, type, content, "{}");
    }

    public List<MemoryEntryEntity> listVisibleMemoriesForPlayer(String roomId, String playerId) {
        return memoryEntryRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .filter(entry -> entry.getScope() == MemoryScope.PUBLIC
                        || playerId.equals(entry.getOwnerPlayerId())
                        || visibleTo(entry, playerId))
                .toList();
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
        return memoryEntryRepository.save(entry);
    }

    private boolean visibleTo(MemoryEntryEntity entry, String playerId) {
        String ids = entry.getVisibleToPlayerIds();
        return ids != null && List.of(ids.split(",")).contains(playerId);
    }
}
