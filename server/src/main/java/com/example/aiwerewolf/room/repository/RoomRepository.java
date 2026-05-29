package com.example.aiwerewolf.room.repository;

import com.example.aiwerewolf.room.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<RoomEntity, String> {
}
