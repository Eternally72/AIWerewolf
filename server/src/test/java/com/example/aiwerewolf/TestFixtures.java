package com.example.aiwerewolf;

import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.memory.entity.MemoryEntryEntity;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.entity.PlayerType;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.room.entity.HumanMode;
import com.example.aiwerewolf.room.entity.ObserverViewMode;
import com.example.aiwerewolf.room.entity.RoomEntity;
import com.example.aiwerewolf.room.entity.RoomStatus;

public final class TestFixtures {
    private TestFixtures() {
    }

    public static PlayerEntity player(String id, String roomId, int seat, Role role) {
        PlayerEntity player = new PlayerEntity();
        player.setId(id);
        player.setRoomId(roomId);
        player.setSeatNumber(seat);
        player.setName("P" + seat);
        player.setType(PlayerType.AI);
        player.setRole(role);
        player.setCamp(role.camp());
        player.setRoleCategory(role.category());
        player.setAlive(true);
        return player;
    }

    public static RoomEntity room(String id) {
        RoomEntity room = new RoomEntity();
        room.setId(id);
        room.setName("room");
        room.setStatus(RoomStatus.RUNNING);
        room.setPhase(GamePhase.DAY_SPEECH);
        room.setTotalSeats(7);
        room.setHumanMode(HumanMode.NONE);
        room.setObserverViewMode(ObserverViewMode.GOD_VIEW);
        return room;
    }

    public static MemoryEntryEntity memory(String id, String roomId, MemoryScope scope, String owner, String content) {
        MemoryEntryEntity entry = new MemoryEntryEntity();
        entry.setId(id);
        entry.setRoomId(roomId);
        entry.setRoundNumber(1);
        entry.setPhase(GamePhase.DAY_SPEECH);
        entry.setScope(scope);
        entry.setOwnerPlayerId(owner);
        entry.setEventType("TEST");
        entry.setContent(content);
        return entry;
    }
}
