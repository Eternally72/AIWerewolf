package com.example.aiwerewolf.game.view;

import com.example.aiwerewolf.aiinfra.context.ContextAssembler;
import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.room.entity.ObserverViewMode;
import com.example.aiwerewolf.room.entity.RoomEntity;
import com.example.aiwerewolf.room.entity.RoomStatus;
import com.example.aiwerewolf.room.repository.RoomRepository;
import com.example.aiwerewolf.speech.repository.SpeechRepository;
import com.example.aiwerewolf.vote.repository.VoteRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class GameViewBuilder {
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final ContextAssembler contextAssembler;
    private final SpeechRepository speechRepository;
    private final VoteRepository voteRepository;

    public GameViewBuilder(RoomRepository roomRepository,
                           PlayerRepository playerRepository,
                           ContextAssembler contextAssembler,
                           SpeechRepository speechRepository,
                           VoteRepository voteRepository) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.contextAssembler = contextAssembler;
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
                room.getCurrentRound(),
                null,
                null,
                null,
                room.getStatus() == RoomStatus.GAME_OVER
                        ? contextAssembler.revealedPlayers(roomId)
                        : contextAssembler.publicPlayers(roomId),
                contextAssembler.publicMemories(roomId).stream().map(MemoryView::of).toList(),
                speechRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream().filter(s -> s.isPublicVisible()).map(SpeechView::of).toList(),
                voteRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream().map(VoteView::of).toList(),
                false
        );
    }

    public GameView buildPrivateView(String roomId, String playerId) {
        RoomEntity room = room(roomId);
        PlayerEntity viewer = findPlayer(playerId)
                .filter(p -> p.getRoomId().equals(roomId))
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_FOUND", "玩家不存在"));
        return new GameView(
                room.getId(),
                room.getName(),
                room.getStatus(),
                room.getPhase(),
                room.getCurrentRound(),
                playerId,
                viewer.getRole(),
                viewer.getCamp(),
                contextAssembler.privatePlayers(roomId, viewer),
                contextAssembler.visibleMemoriesForPlayer(roomId, playerId).stream().map(MemoryView::of).toList(),
                speechRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream().filter(s -> s.isPublicVisible()).map(SpeechView::of).toList(),
                voteRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream().map(VoteView::of).toList(),
                false
        );
    }

    public GameView buildWerewolfTeamView(String roomId, String playerId) {
        return buildPrivateView(roomId, playerId);
    }

    public GameView buildGodView(String roomId) {
        RoomEntity room = room(roomId);
        return new GameView(
                room.getId(),
                room.getName(),
                room.getStatus(),
                room.getPhase(),
                room.getCurrentRound(),
                null,
                null,
                null,
                contextAssembler.godPlayers(roomId),
                contextAssembler.godMemories(roomId).stream().map(MemoryView::of).toList(),
                speechRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream().map(SpeechView::of).toList(),
                voteRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream().map(VoteView::of).toList(),
                true
        );
    }

    public GameView buildObserverView(String roomId, ObserverViewMode mode) {
        return mode == ObserverViewMode.GOD_VIEW ? buildGodView(roomId) : buildPublicView(roomId);
    }

    private Optional<PlayerEntity> findPlayer(String playerId) {
        return playerRepository.findById(Objects.requireNonNull(playerId, "playerId must not be null"));
    }

    private RoomEntity room(String roomId) {
        return roomRepository.findById(Objects.requireNonNull(roomId, "roomId must not be null"))
                .orElseThrow(() -> new BusinessException("ROOM_NOT_FOUND", "房间不存在"));
    }
}
