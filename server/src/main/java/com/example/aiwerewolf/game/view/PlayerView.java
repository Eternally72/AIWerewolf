package com.example.aiwerewolf.game.view;

import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.entity.PlayerType;
import com.example.aiwerewolf.role.model.Camp;
import com.example.aiwerewolf.role.model.Role;

public record PlayerView(
        String id,
        int seatNumber,
        String name,
        PlayerType type,
        boolean alive,
        boolean canSpeak,
        boolean canVote,
        Role role,
        Camp camp
) {
    public static PlayerView publicOf(PlayerEntity player) {
        return new PlayerView(
                player.getId(),
                player.getSeatNumber(),
                player.getName(),
                player.getType(),
                player.isAlive(),
                player.isCanSpeak(),
                player.isCanVote(),
                null,
                null
        );
    }

    public static PlayerView fullOf(PlayerEntity player) {
        return new PlayerView(
                player.getId(),
                player.getSeatNumber(),
                player.getName(),
                player.getType(),
                player.isAlive(),
                player.isCanSpeak(),
                player.isCanVote(),
                player.getRole(),
                player.getCamp()
        );
    }
}
