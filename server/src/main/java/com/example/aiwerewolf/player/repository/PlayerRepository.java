package com.example.aiwerewolf.player.repository;

import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.role.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRepository extends JpaRepository<PlayerEntity, String> {
    List<PlayerEntity> findByRoomIdOrderBySeatNumberAsc(String roomId);
    List<PlayerEntity> findByRoomIdAndAliveTrueOrderBySeatNumberAsc(String roomId);
    List<PlayerEntity> findByRoomIdAndRoleIn(String roomId, List<Role> roles);
}
