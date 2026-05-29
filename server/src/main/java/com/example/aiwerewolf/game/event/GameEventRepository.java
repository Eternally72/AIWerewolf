package com.example.aiwerewolf.game.event;

import com.example.aiwerewolf.memory.entity.MemoryScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameEventRepository extends JpaRepository<GameEventEntity, String> {
    List<GameEventEntity> findByRoomIdAndScopeOrderByCreatedAtAsc(String roomId, MemoryScope scope);
    List<GameEventEntity> findByRoomIdOrderByCreatedAtAsc(String roomId);
}
