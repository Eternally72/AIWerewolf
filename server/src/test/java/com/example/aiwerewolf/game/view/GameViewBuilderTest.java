package com.example.aiwerewolf.game.view;

import com.example.aiwerewolf.aiinfra.context.ContextAssembler;
import com.example.aiwerewolf.aiinfra.context.ContextBudgetPolicy;
import com.example.aiwerewolf.aiinfra.context.MemoryAccessPolicy;
import com.example.aiwerewolf.TestFixtures;
import com.example.aiwerewolf.memory.entity.MemoryEntryEntity;
import com.example.aiwerewolf.memory.entity.MemoryScope;
import com.example.aiwerewolf.memory.repository.MemoryEntryRepository;
import com.example.aiwerewolf.player.entity.PlayerEntity;
import com.example.aiwerewolf.player.repository.PlayerRepository;
import com.example.aiwerewolf.role.model.LoverRelationEntity;
import com.example.aiwerewolf.role.model.Role;
import com.example.aiwerewolf.role.model.LoverRelationRepository;
import com.example.aiwerewolf.room.repository.RoomRepository;
import com.example.aiwerewolf.room.entity.RoomEntity;
import com.example.aiwerewolf.room.entity.RoomStatus;
import com.example.aiwerewolf.speech.repository.SpeechRepository;
import com.example.aiwerewolf.vote.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameViewBuilderTest {
    private final RoomRepository roomRepository = mock(RoomRepository.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final MemoryEntryRepository memoryEntryRepository = mock(MemoryEntryRepository.class);
    private final SpeechRepository speechRepository = mock(SpeechRepository.class);
    private final VoteRepository voteRepository = mock(VoteRepository.class);
    private final LoverRelationRepository loverRelationRepository = mock(LoverRelationRepository.class);
    private GameViewBuilder builder;

    private final String roomId = "room";
    private PlayerEntity wolf;
    private PlayerEntity villager;
    private PlayerEntity seer;

    @BeforeEach
    void setUp() {
        ContextAssembler contextAssembler = new ContextAssembler(
                playerRepository,
                memoryEntryRepository,
                loverRelationRepository,
                new MemoryAccessPolicy(),
                new ContextBudgetPolicy());
        builder = new GameViewBuilder(roomRepository, playerRepository, contextAssembler, speechRepository, voteRepository);
        wolf = TestFixtures.player("wolf", roomId, 1, Role.WEREWOLF);
        villager = TestFixtures.player("villager", roomId, 2, Role.VILLAGER);
        seer = TestFixtures.player("seer", roomId, 3, Role.SEER);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(TestFixtures.room(roomId)));
        when(playerRepository.findByRoomIdOrderBySeatNumberAsc(roomId)).thenReturn(List.of(wolf, villager, seer));
        when(playerRepository.findById("wolf")).thenReturn(Optional.of(wolf));
        when(playerRepository.findById("villager")).thenReturn(Optional.of(villager));
        when(playerRepository.findById("seer")).thenReturn(Optional.of(seer));
        when(speechRepository.findByRoomIdOrderByCreatedAtAsc(roomId)).thenReturn(List.of());
        when(voteRepository.findByRoomIdOrderByCreatedAtAsc(roomId)).thenReturn(List.of());
        when(loverRelationRepository.findByRoomId(roomId)).thenReturn(List.of());
    }

    @Test
    void publicViewDoesNotContainPrivateRoles() {
        when(memoryEntryRepository.findByRoomIdAndScopeOrderByCreatedAtAsc(roomId, MemoryScope.PUBLIC)).thenReturn(List.of());

        GameView view = builder.buildPublicView(roomId);

        assertThat(view.players()).allMatch(p -> p.role() == null && p.camp() == null);
        assertThat(view.godView()).isFalse();
    }

    @Test
    void publicViewRevealsRolesAfterGameOver() {
        RoomEntity room = TestFixtures.room(roomId);
        room.setStatus(RoomStatus.GAME_OVER);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(memoryEntryRepository.findByRoomIdAndScopeOrderByCreatedAtAsc(roomId, MemoryScope.PUBLIC)).thenReturn(List.of());

        GameView view = builder.buildPublicView(roomId);

        assertThat(view.players()).filteredOn(p -> p.id().equals("wolf")).first().extracting(PlayerView::role).isEqualTo(Role.WEREWOLF);
        assertThat(view.players()).filteredOn(p -> p.id().equals("seer")).first().extracting(PlayerView::role).isEqualTo(Role.SEER);
    }

    @Test
    void villagerCannotSeeWolfIdentity() {
        when(memoryEntryRepository.findByRoomIdOrderByCreatedAtAsc(roomId)).thenReturn(List.of());

        GameView view = builder.buildPrivateView(roomId, "villager");

        assertThat(view.ownRole()).isEqualTo(Role.VILLAGER);
        assertThat(view.players()).filteredOn(p -> p.id().equals("wolf")).first().extracting(PlayerView::role).isNull();
    }

    @Test
    void wolfCanSeeWolfIdentityButNotGoodPrivateRoles() {
        when(memoryEntryRepository.findByRoomIdOrderByCreatedAtAsc(roomId)).thenReturn(List.of());

        GameView view = builder.buildPrivateView(roomId, "wolf");

        assertThat(view.players()).filteredOn(p -> p.id().equals("wolf")).first().extracting(PlayerView::role).isEqualTo(Role.WEREWOLF);
        assertThat(view.players()).filteredOn(p -> p.id().equals("seer")).first().extracting(PlayerView::role).isNull();
    }

    @Test
    void privateMemoryOnlyVisibleToOwnerAndGodViewSeesAll() {
        MemoryEntryEntity publicEntry = TestFixtures.memory("m1", roomId, MemoryScope.PUBLIC, null, "public");
        MemoryEntryEntity privateEntry = TestFixtures.memory("m2", roomId, MemoryScope.PRIVATE, "seer", "check result");
        when(memoryEntryRepository.findByRoomIdOrderByCreatedAtAsc(roomId)).thenReturn(List.of(publicEntry, privateEntry));

        GameView villagerView = builder.buildPrivateView(roomId, "villager");
        GameView godView = builder.buildGodView(roomId);

        assertThat(villagerView.memories()).extracting(MemoryView::content).containsExactly("public");
        assertThat(godView.memories()).extracting(MemoryView::content).contains("public", "check result");
        assertThat(godView.players()).filteredOn(p -> p.id().equals("seer")).first().extracting(PlayerView::role).isEqualTo(Role.SEER);
    }

    @Test
    void loverCanSeePartnerIdentity() {
        LoverRelationEntity relation = new LoverRelationEntity();
        relation.setRoomId(roomId);
        relation.setPlayerAId("villager");
        relation.setPlayerBId("seer");
        relation.setThirdPartyMode(false);
        when(loverRelationRepository.findByRoomId(roomId)).thenReturn(List.of(relation));
        when(memoryEntryRepository.findByRoomIdOrderByCreatedAtAsc(roomId)).thenReturn(List.of());

        GameView view = builder.buildPrivateView(roomId, "villager");

        assertThat(view.players()).filteredOn(p -> p.id().equals("seer")).first().extracting(PlayerView::role).isEqualTo(Role.SEER);
        assertThat(view.players()).filteredOn(p -> p.id().equals("wolf")).first().extracting(PlayerView::role).isNull();
    }
}
