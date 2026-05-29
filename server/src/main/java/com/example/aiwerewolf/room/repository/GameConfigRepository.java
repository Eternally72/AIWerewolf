package com.example.aiwerewolf.room.repository;

import com.example.aiwerewolf.room.entity.GameConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameConfigRepository extends JpaRepository<GameConfigEntity, String> {
    Optional<GameConfigEntity> findByRoomId(String roomId);
}
