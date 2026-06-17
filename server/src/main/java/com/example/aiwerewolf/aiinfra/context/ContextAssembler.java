package com.example.aiwerewolf.aiinfra.context;

import com.example.aiwerewolf.game.view.PlayerView;
import com.example.aiwerewolf.memory.entity.MemoryEntryEntity;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import com.example.aiwerewolf.memory.repository.MemoryEntryRepository;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.role.model.LoverRelationEntity;
import com.example.aiwerewolf.role.model.LoverRelationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContextAssembler {
    private final PlayerRepository playerRepository;
    private final MemoryEntryRepository memoryEntryRepository;
    private final LoverRelationRepository loverRelationRepository;
    private final MemoryAccessPolicy memoryAccessPolicy;
    private final ContextBudgetPolicy contextBudgetPolicy;

    public ContextAssembler(PlayerRepository playerRepository,
                            MemoryEntryRepository memoryEntryRepository,
                            LoverRelationRepository loverRelationRepository,
                            MemoryAccessPolicy memoryAccessPolicy,
                            ContextBudgetPolicy contextBudgetPolicy) {
        this.playerRepository = playerRepository;
        this.memoryEntryRepository = memoryEntryRepository;
        this.loverRelationRepository = loverRelationRepository;
        this.memoryAccessPolicy = memoryAccessPolicy;
        this.contextBudgetPolicy = contextBudgetPolicy;
    }

    public List<PlayerView> publicPlayers(String roomId) {
        return playerRepository.findByRoomIdOrderBySeatNumberAsc(roomId).stream()
                .map(PlayerView::publicOf)
                .toList();
    }

    public List<PlayerView> revealedPlayers(String roomId) {
        return playerRepository.findByRoomIdOrderBySeatNumberAsc(roomId).stream()
                .map(PlayerView::fullOf)
                .toList();
    }

    public List<PlayerView> privatePlayers(String roomId, PlayerEntity viewer) {
        Set<String> loverIds = loverIds(roomId, viewer.getId());
        return playerRepository.findByRoomIdOrderBySeatNumberAsc(roomId).stream()
                .map(player -> memoryAccessPolicy.canViewPlayerIdentity(viewer, player, loverIds)
                        ? PlayerView.fullOf(player)
                        : PlayerView.publicOf(player))
                .toList();
    }

    public List<PlayerView> godPlayers(String roomId) {
        return playerRepository.findByRoomIdOrderBySeatNumberAsc(roomId).stream()
                .map(PlayerView::fullOf)
                .toList();
    }

    public List<MemoryEntryEntity> publicMemories(String roomId) {
        return contextBudgetPolicy.fitVisibleMemories(
                memoryEntryRepository.findByRoomIdAndScopeOrderByCreatedAtAsc(roomId, MemoryScope.PUBLIC));
    }

    public List<MemoryEntryEntity> visibleMemoriesForPlayer(String roomId, String playerId) {
        List<MemoryEntryEntity> visible = memoryEntryRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .filter(entry -> memoryAccessPolicy.canViewMemory(entry, playerId, false))
                .toList();
        return contextBudgetPolicy.fitVisibleMemories(visible);
    }

    public List<MemoryEntryEntity> godMemories(String roomId) {
        return memoryEntryRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
    }

    private Set<String> loverIds(String roomId, String viewerId) {
        return loverRelationRepository.findByRoomId(roomId).stream()
                .filter(relation -> relation.getPlayerAId().equals(viewerId) || relation.getPlayerBId().equals(viewerId))
                .map(relation -> partnerId(relation, viewerId))
                .collect(Collectors.toUnmodifiableSet());
    }

    private String partnerId(LoverRelationEntity relation, String viewerId) {
        return relation.getPlayerAId().equals(viewerId) ? relation.getPlayerBId() : relation.getPlayerAId();
    }
}
