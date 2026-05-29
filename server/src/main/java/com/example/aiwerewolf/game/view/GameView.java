package com.example.aiwerewolf.game.view;

import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.role.model.Camp;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.room.entity.RoomStatus;

import java.util.List;

public record GameView(
        String roomId,
        String roomName,
        RoomStatus status,
        GamePhase phase,
        int roundNumber,
        String viewerPlayerId,
        Role ownRole,
        Camp ownCamp,
        List<PlayerView> players,
        List<MemoryView> memories,
        List<SpeechView> speeches,
        List<VoteView> votes,
        boolean godView
) {
}
