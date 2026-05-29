package com.example.aiwerewolf.game.view;

import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import com.example.aiwerewolf.memory.repository.MemoryEntryRepository;
import com.example.aiwerewolf.memory.service.MemoryService;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.room.entity.ObserverViewMode;
import com.example.aiwerewolf.room.entity.RoomEntity;
import com.example.aiwerewolf.room.repository.RoomRepository;
import com.example.aiwerewolf.speech.repository.SpeechRepository;
import com.example.aiwerewolf.vote.repository.VoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameViewBuilder {
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final MemoryService memoryService;
    private final MemoryEntryRepository memoryEntryRepository;
    private final SpeechRepository speechRepository;
    private final VoteRepository voteRepository;

    public GameViewBuilder(RoomRepository roomRepository,
                           PlayerRepository playerRepository,
                           MemoryService memoryService,
                           MemoryEntryRepository memoryEntryRepository,
                           SpeechRepository speechRepository,
                           VoteRepository voteRepository) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.memoryService = memoryService;
        this.memoryEntryRepository = memoryEntryRepository;
        this.speechRepository = speechRepository;
        this.voteRepository = voteRepository;
    }

    public GameView buildPublicView(String roomId) {
        RoomEntity room = room(roomId);
        return new GameView(
                room.getId(),
                room.getName(),
                room.getStatus(),
                room.getPhase(),
                1,
                null,
                null,
                null,
                playerRepository.findByRoomIdOrderBySeatNumberAsc(roomId).stream().map(PlayerView::publicOf).toList(),
                memoryEntryRepository.findByRoomIdAndScopeOrderByCreatedAtAsc(roomId, MemoryScope.PUBLIC).stream().map(MemoryView::of).toList(),
                speechRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream().filter(s -> s.isPublicVisible()).map(SpeechView::of).toList(),
                voteRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream().map(VoteView::of).toList(),
                false
        );
    }

    public GameView buildPrivateView(String roomId, String playerId) {
        RoomEntity room = room(roomId);
        PlayerEntity viewer = playerRepository.findById(playerId)
                .filter(p -> p.getRoomId().equals(roomId))
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_FOUND", "玩家不存在"));
        return new GameView(
                room.getId(),
                room.getName(),
                room.getStatus(),
                room.getPhase(),
                1,
                playerId,
                viewer.getRole(),
                viewer.getCamp(),
                privatePlayers(roomId, viewer),
                memoryService.listVisibleMemoriesForPlayer(roomId, playerId).stream().map(MemoryView::of).toList(),
                speechRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream().filter(s -> s.isPublicVisible()).map(SpeechView::of).toList(),
                voteRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream().map(VoteView::of).toList(),
                false
        );
    }

    public GameView buildWerewolfTeamView(String roomId, String playerId) {
        GameView privateView = buildPrivateView(roomId, playerId);
        if (privateView.ownCamp() == null || !privateView.ownCamp().name().equals("WEREWOLF")) {
            return privateView;
        }
        return privateView;
    }

    public GameView buildGodView(String roomId) {
        RoomEntity room = room(roomId);
        return new GameView(
                room.getId(),
                room.getName(),
                room.getStatus(),
                room.getPhase(),
                1,
                null,
                null,
                null,
                playerRepository.findByRoomIdOrderBySeatNumberAsc(roomId).stream().map(PlayerView::fullOf).toList(),
                memoryService.listGodViewMemories(roomId).stream().map(MemoryView::of).toList(),
                speechRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream().map(SpeechView::of).toList(),
                voteRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream().map(VoteView::of).toList(),
                true
        );
    }

    public GameView buildObserverView(String roomId, ObserverViewMode mode) {
        return mode == ObserverViewMode.GOD_VIEW ? buildGodView(roomId) : buildPublicView(roomId);
    }

    private List<PlayerView> privatePlayers(String roomId, PlayerEntity viewer) {
        boolean wolf = viewer.getRole() != null && viewer.getRole().isWerewolfCamp();
        return playerRepository.findByRoomIdOrderBySeatNumberAsc(roomId).stream()
                .map(player -> {
                    if (player.getId().equals(viewer.getId()) || (wolf && player.getRole() != null && player.getRole().isWerewolfCamp())) {
                        return PlayerView.fullOf(player);
                    }
                    return PlayerView.publicOf(player);
                })
                .toList();
    }

    private RoomEntity room(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException("ROOM_NOT_FOUND", "房间不存在"));
    }
}
