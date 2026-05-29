package com.example.aiwerewolf.memory.repository;

import com.example.aiwerewolf.memory.entity.MemoryEntryEntity;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoryEntryRepository extends JpaRepository<MemoryEntryEntity, String> {
    List<MemoryEntryEntity> findByRoomIdAndScopeOrderByCreatedAtAsc(String roomId, MemoryScope scope);
    List<MemoryEntryEntity> findByRoomIdOrderByCreatedAtAsc(String roomId);
    List<MemoryEntryEntity> findByRoomIdAndOwnerPlayerIdOrderByCreatedAtAsc(String roomId, String ownerPlayerId);
}
