package com.example.aiwerewolf.game.engine;

import com.example.aiwerewolf.aiinfra.observability.AiInfraMetrics;
import com.example.aiwerewolf.TestFixtures;
import com.example.aiwerewolf.action.repository.GameActionRepository;
import com.example.aiwerewolf.action.service.DeathResolutionService;
import com.example.aiwerewolf.action.service.NightActionService;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.rule.VictoryConditionService;
import com.example.aiwerewolf.game.runtime.GameRuntimeStateCache;
import com.example.aiwerewolf.game.runtime.IdempotencyService;
import com.example.aiwerewolf.game.runtime.PhaseAdvanceLockService;
import com.example.aiwerewolf.memory.service.MemoryService;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.room.entity.RoomEntity;
import com.example.aiwerewolf.room.repository.RoomRepository;
import com.example.aiwerewolf.speech.repository.SpeechRepository;
import com.example.aiwerewolf.speech.service.SpeechService;
import com.example.aiwerewolf.vote.repository.VoteRepository;
import com.example.aiwerewolf.vote.service.VoteService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SevenPlayerAutoGameSimulationTest {
    @Test
    void fullAiSevenPlayerRunCanAdvanceToGameOver() {
        String roomId = "seven-ai";
        RoomEntity room = TestFixtures.room(roomId);
        room.setPhase(GamePhase.FIRST_NIGHT);

        PlayerEntity wolf1 = TestFixtures.player("wolf1", roomId, 1, Role.WEREWOLF);
        PlayerEntity wolf2 = TestFixtures.player("wolf2", roomId, 2, Role.WEREWOLF);
        List<PlayerEntity> players = List.of(
                wolf1,
                wolf2,
                TestFixtures.player("villager1", roomId, 3, Role.VILLAGER),
                TestFixtures.player("villager2", roomId, 4, Role.VILLAGER),
                TestFixtures.player("villager3", roomId, 5, Role.VILLAGER),
                TestFixtures.player("seer", roomId, 6, Role.SEER),
                TestFixtures.player("witch", roomId, 7, Role.WITCH)
        );

        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        NightActionService nightActionService = mock(NightActionService.class);
        PhaseAdvanceLockService lockService = mock(PhaseAdvanceLockService.class);
        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        GameRuntimeStateCache runtimeStateCache = mock(GameRuntimeStateCache.class);
        MemoryService memoryService = mock(MemoryService.class);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(RoomEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerRepository.findByRoomIdAndAliveTrueOrderBySeatNumberAsc(roomId)).thenReturn(players);
        when(playerRepository.findByRoomIdOrderBySeatNumberAsc(roomId)).thenReturn(players);
        when(idempotencyService.markIfAbsent(anyString(), any(Duration.class))).thenReturn(true);
        when(lockService.withRoomLock(eq(roomId), any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        doAnswer(invocation -> {
            wolf1.setAlive(false);
            wolf2.setAlive(false);
            return null;
        }).when(nightActionService).resolveNightActions(roomId, 1);

        GamePhaseEngine engine = new GamePhaseEngine(
                roomRepository,
                playerRepository,
                mock(GameActionRepository.class),
                mock(SpeechRepository.class),
                mock(VoteRepository.class),
                nightActionService,
                mock(SpeechService.class),
                mock(VoteService.class),
                mock(DeathResolutionService.class),
                new VictoryConditionService(),
                memoryService,
                lockService,
                idempotencyService,
                runtimeStateCache,
                new AiInfraMetrics(new SimpleMeterRegistry())
        );

        RoomEntity result = engine.advanceUntilHumanInputRequired(roomId);

        assertThat(result.getPhase()).isEqualTo(GamePhase.GAME_OVER);
        verify(nightActionService, never()).generateAiNightActions(roomId, 1, GamePhase.GUARD_ACTION);
        verify(nightActionService).generateAiNightActions(roomId, 1, GamePhase.WEREWOLF_ACTION);
        verify(nightActionService).generateAiNightActions(roomId, 1, GamePhase.SEER_ACTION);
        verify(nightActionService).generateAiNightActions(roomId, 1, GamePhase.WITCH_ACTION);
        verify(nightActionService).resolveNightActions(roomId, 1);
    }
}
