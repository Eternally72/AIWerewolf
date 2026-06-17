package com.example.aiwerewolf.game.engine;

import com.example.aiwerewolf.aiinfra.observability.AiInfraMetrics;
import com.example.aiwerewolf.TestFixtures;
import com.example.aiwerewolf.action.service.DeathResolutionService;
import com.example.aiwerewolf.action.service.NightActionService;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.runtime.GameRuntimeStateCache;
import com.example.aiwerewolf.game.runtime.IdempotencyService;
import com.example.aiwerewolf.game.runtime.PhaseAdvanceLockService;
import com.example.aiwerewolf.game.rule.VictoryConditionService;
import com.example.aiwerewolf.memory.service.MemoryService;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.room.entity.RoomEntity;
import com.example.aiwerewolf.room.repository.RoomRepository;
import com.example.aiwerewolf.speech.service.SpeechService;
import com.example.aiwerewolf.vote.service.VoteService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GamePhaseEngineTest {
    @Test
    void phaseTransitionOrderIsExplicit() {
        GamePhaseEngine engine = new GamePhaseEngine(
                mock(RoomRepository.class),
                mock(PlayerRepository.class),
                mock(NightActionService.class),
                mock(SpeechService.class),
                mock(VoteService.class),
                mock(DeathResolutionService.class),
                new VictoryConditionService(),
                mock(MemoryService.class),
                mock(PhaseAdvanceLockService.class),
                mock(IdempotencyService.class),
                mock(GameRuntimeStateCache.class),
                new AiInfraMetrics(new SimpleMeterRegistry())
        );

        assertThat(engine.validatePhaseTransition(GamePhase.FIRST_NIGHT, GamePhase.GUARD_ACTION)).isTrue();
        assertThat(engine.validatePhaseTransition(GamePhase.DAY_VOTE, GamePhase.EXECUTION)).isTrue();
        assertThat(engine.validatePhaseTransition(GamePhase.DAY_VOTE, GamePhase.NIGHT)).isFalse();
    }

    @Test
    void autoAdvanceDoesNotStopAtNightRolePhaseForVillagerHuman() {
        String roomId = "room";
        RoomEntity room = TestFixtures.room(roomId);
        room.setPhase(GamePhase.SEER_ACTION);
        PlayerEntity humanVillager = TestFixtures.player("human", roomId, 1, Role.VILLAGER);
        humanVillager.setType(com.example.aiwerewolf.player.entity.PlayerType.HUMAN);
        PlayerEntity wolf = TestFixtures.player("wolf", roomId, 2, Role.WEREWOLF);
        PlayerEntity seer = TestFixtures.player("seer", roomId, 3, Role.SEER);

        RoomRepository roomRepository = mock(RoomRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        NightActionService nightActionService = mock(NightActionService.class);
        PhaseAdvanceLockService lockService = mock(PhaseAdvanceLockService.class);
        IdempotencyService idempotencyService = mock(IdempotencyService.class);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(RoomEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerRepository.findByRoomIdAndAliveTrueOrderBySeatNumberAsc(roomId)).thenReturn(List.of(humanVillager, wolf, seer));
        when(playerRepository.findByRoomIdOrderBySeatNumberAsc(roomId)).thenReturn(List.of(humanVillager, wolf, seer));
        when(idempotencyService.markIfAbsent(anyString(), any(Duration.class))).thenReturn(true);
        when(lockService.withRoomLock(eq(roomId), any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });

        GamePhaseEngine engine = new GamePhaseEngine(
                roomRepository,
                playerRepository,
                nightActionService,
                mock(SpeechService.class),
                mock(VoteService.class),
                mock(DeathResolutionService.class),
                new VictoryConditionService(),
                mock(MemoryService.class),
                lockService,
                idempotencyService,
                mock(GameRuntimeStateCache.class),
                new AiInfraMetrics(new SimpleMeterRegistry())
        );

        RoomEntity result = engine.advanceUntilHumanInputRequired(roomId);

        assertThat(result.getPhase()).isEqualTo(GamePhase.DAY_SPEECH);
        verify(nightActionService).generateAiNightActions(roomId, 1, GamePhase.SEER_ACTION);
        verify(nightActionService).generateAiNightActions(roomId, 1, GamePhase.WITCH_ACTION);
    }
}
