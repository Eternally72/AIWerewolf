package com.example.aiwerewolf.action.service;

import com.example.aiwerewolf.memory.service.MemoryService;
import com.example.aiwerewolf.player.entity.DeathReason;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.game.phase.GamePhase;
import org.springframework.stereotype.Service;

@Service
public class DeathResolutionService {
    private final PlayerRepository playerRepository;
    private final MemoryService memoryService;

    public DeathResolutionService(PlayerRepository playerRepository, MemoryService memoryService) {
        this.playerRepository = playerRepository;
        this.memoryService = memoryService;
    }

    public void killPlayer(PlayerEntity player, String roomId, int round, GamePhase phase, DeathReason reason) {
        if (!player.isAlive()) {
            return;
        }
        player.setAlive(false);
        player.setCanSpeak(false);
        player.setCanVote(false);
        player.setDeathReason(reason);
        player.setDeathRound(round);
        playerRepository.save(player);
        memoryService.appendPublicMemory(roomId, round, phase, "PLAYER_DEAD",
                "玩家 " + player.getSeatNumber() + " 号 " + player.getName() + " 死亡");
        memoryService.appendGodViewMemory(roomId, round, phase, "DEATH_DETAIL",
                player.getName() + " 死亡，原因：" + reason + "，身份：" + player.getRole());
    }

    public void exilePlayer(PlayerEntity player, String roomId, int round) {
        killPlayer(player, roomId, round, GamePhase.EXECUTION, DeathReason.VOTE_EXILE);
    }
}
