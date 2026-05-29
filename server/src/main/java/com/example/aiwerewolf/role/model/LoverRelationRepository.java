package com.example.aiwerewolf.role.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoverRelationRepository extends JpaRepository<LoverRelationEntity, String> {
    List<LoverRelationEntity> findByRoomId(String roomId);
}
