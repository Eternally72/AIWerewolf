package com.example.aiwerewolf.vote.repository;

import com.example.aiwerewolf.vote.entity.VoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<VoteEntity, String> {
    List<VoteEntity> findByRoomIdAndRoundNumber(String roomId, int roundNumber);
    List<VoteEntity> findByRoomIdOrderByCreatedAtAsc(String roomId);
    Optional<VoteEntity> findByRoomIdAndRoundNumberAndVoterPlayerId(String roomId, int roundNumber, String voterPlayerId);
}
