package com.example.aiwerewolf.speech.repository;

import com.example.aiwerewolf.speech.entity.SpeechEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpeechRepository extends JpaRepository<SpeechEntity, String> {
    List<SpeechEntity> findByRoomIdOrderByCreatedAtAsc(String roomId);
    List<SpeechEntity> findByRoomIdAndRoundNumberOrderByCreatedAtAsc(String roomId, int roundNumber);
    Optional<SpeechEntity> findByRoomIdAndRoundNumberAndPlayerId(String roomId, int roundNumber, String playerId);
}
