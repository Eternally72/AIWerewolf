package com.example.aiwerewolf.game.engine;

import com.example.aiwerewolf.TestFixtures;
import com.example.aiwerewolf.action.entity.ActionType;
import com.example.aiwerewolf.common.exception.BusinessException;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.room.entity.RoomEntity;
import com.example.aiwerewolf.room.repository.RoomRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameOperationValidatorTest {
    private final RoomRepository roomRepository = mock(RoomRepository.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final GameOperationValidator validator = new GameOperationValidator(roomRepository, playerRepository);

    @Test
    void speechOutsideDaySpeechIsRejected() {
        RoomEntity room = TestFixtures.room("room");
        room.setPhase(GamePhase.DAY_VOTE);
        when(roomRepository.findById("room")).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> validator.requirePhase("room", GamePhase.DAY_SPEECH))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能执行");
    }

    @Test
    void nightActionOutsideMatchingPhaseIsRejected() {
        RoomEntity room = TestFixtures.room("room");
        room.setPhase(GamePhase.SEER_ACTION);
        when(roomRepository.findById("room")).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> validator.requireNightActionPhase("room", ActionType.KILL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能执行");
    }

    @Test
    void targetMustBelongToSameRoomAndBeAlive() {
        PlayerEntity target = TestFixtures.player("target", "another-room", 1, Role.VILLAGER);
        when(playerRepository.findById("target")).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> validator.requireAliveTargetInRoom("room", "target"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不属于当前房间");
    }
}
