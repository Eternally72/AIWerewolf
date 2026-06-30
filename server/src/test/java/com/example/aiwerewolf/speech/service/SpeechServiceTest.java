package com.example.aiwerewolf.speech.service;

import com.example.aiwerewolf.TestFixtures;
import com.example.aiwerewolf.agent.core.AiAgentService;
import com.example.aiwerewolf.agent.dto.AiSpeechDecision;
import com.example.aiwerewolf.aiinfra.worker.AgentTaskService;
import com.example.aiwerewolf.game.engine.GameOperationValidator;
import com.example.aiwerewolf.game.phase.GamePhase;
import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.game.view.GameViewBuilder;
import com.example.aiwerewolf.game.view.SpeechView;
import com.example.aiwerewolf.memory.service.MemoryService;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.role.model.Camp;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.room.entity.RoomStatus;
import com.example.aiwerewolf.speech.entity.SpeechEntity;
import com.example.aiwerewolf.speech.repository.SpeechRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpeechServiceTest {
    @Test
    void processesOneSpeakerPerStepAndNextSpeakerSeesPreviousSpeech() {
        String roomId = "room";
        PlayerEntity first = TestFixtures.player("p1", roomId, 1, Role.VILLAGER);
        PlayerEntity second = TestFixtures.player("p2", roomId, 2, Role.SEER);
        List<SpeechEntity> stored = new ArrayList<>();
        AtomicReference<GameView> secondInput = new AtomicReference<>();

        SpeechRepository speechRepository = mock(SpeechRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        AiAgentService aiAgentService = mock(AiAgentService.class);
        GameViewBuilder gameViewBuilder = mock(GameViewBuilder.class);
        AgentTaskService agentTaskService = mock(AgentTaskService.class);

        when(playerRepository.findByRoomIdAndAliveTrueOrderBySeatNumberAsc(roomId)).thenReturn(List.of(first, second));
        when(playerRepository.findById("p1")).thenReturn(Optional.of(first));
        when(playerRepository.findById("p2")).thenReturn(Optional.of(second));
        when(speechRepository.findByRoomIdOrderByCreatedAtAsc(roomId)).thenAnswer(invocation -> List.copyOf(stored));
        when(speechRepository.findByRoomIdAndRoundNumberAndPlayerId(anyString(), anyInt(), anyString()))
                .thenAnswer(invocation -> stored.stream()
                        .filter(speech -> speech.getRoomId().equals(invocation.getArgument(0)))
                        .filter(speech -> speech.getRoundNumber() == (int) invocation.getArgument(1))
                        .filter(speech -> speech.getPlayerId().equals(invocation.getArgument(2)))
                        .findFirst());
        when(speechRepository.saveAndFlush(any(SpeechEntity.class))).thenAnswer(invocation -> {
            SpeechEntity speech = invocation.getArgument(0);
            speech.prePersist();
            stored.add(speech);
            return speech;
        });
        when(agentTaskService.execute(any(), any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(gameViewBuilder.buildPrivateView(roomId, "p1")).thenAnswer(invocation -> view(roomId, first, stored));
        when(gameViewBuilder.buildPrivateView(roomId, "p2")).thenAnswer(invocation -> {
            GameView view = view(roomId, second, stored);
            secondInput.set(view);
            return view;
        });
        when(aiAgentService.generateSpeech(anyString(), any(GameView.class))).thenAnswer(invocation ->
                new AiSpeechDecision("来自" + invocation.getArgument(0) + "的发言", "", "测试策略"));

        SpeechService service = new SpeechService(
                speechRepository,
                playerRepository,
                aiAgentService,
                gameViewBuilder,
                mock(MemoryService.class),
                mock(GameOperationValidator.class),
                agentTaskService,
                new ObjectMapper());

        boolean firstStepCompletedPhase = service.processNextAiSpeech(roomId, 1);
        assertThat(firstStepCompletedPhase).isFalse();
        assertThat(stored).hasSize(1);

        boolean secondStepCompletedPhase = service.processNextAiSpeech(roomId, 1);

        assertThat(stored).hasSize(2);
        assertThat(secondStepCompletedPhase).isTrue();
        assertThat(secondInput.get().speeches())
                .extracting(SpeechView::playerId, SpeechView::content)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("p1", "来自p1的发言"));
    }

    private GameView view(String roomId, PlayerEntity viewer, List<SpeechEntity> stored) {
        return new GameView(
                roomId,
                "room",
                RoomStatus.RUNNING,
                GamePhase.DAY_SPEECH,
                1,
                viewer.getId(),
                viewer.getRole(),
                Camp.GOOD,
                List.of(),
                List.of(),
                stored.stream().map(SpeechView::of).toList(),
                List.of(),
                false);
    }
}
